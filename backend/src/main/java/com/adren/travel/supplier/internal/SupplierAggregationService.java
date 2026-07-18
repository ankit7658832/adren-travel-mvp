package com.adren.travel.supplier.internal;

import com.adren.travel.security.CurrentPrincipal;
import com.adren.travel.supplier.ActivateLocalDmcCommand;
import com.adren.travel.supplier.LocalDmcView;
import com.adren.travel.supplier.SubmitLocalDmcCommand;
import com.adren.travel.supplier.SupplierCredentialSummary;
import com.adren.travel.supplier.SupplierId;
import com.adren.travel.supplier.SupplierSearchApi;
import com.adren.travel.supplier.SupplierSearchResult;
import com.adren.travel.supplier.UpdateSupplierCredentialCommand;
import com.adren.travel.supplier.internal.hotelbeds.HotelbedsClient;
import com.adren.travel.supplier.internal.stuba.StubaClient;
import com.adren.travel.supplier.internal.tbo.TboClient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Aggregates all connected suppliers behind {@link SupplierSearchApi}.
 * Per PRD Section 24.2 (NFR): each supplier call is isolated behind its own
 * named circuit breaker (BOK-26's {@link SupplierCircuitBreakerGateway}) and
 * fanned out in parallel with a bounded per-call timeout, so one supplier's
 * downtime or latency can't degrade — or block — the others.
 */
@Service
class SupplierAggregationService implements SupplierSearchApi {

    private static final long SUPPLIER_CALL_TIMEOUT_SECONDS = 5;

    private final HotelbedsClient hotelbedsClient;
    private final StubaClient stubaClient;
    private final TboClient tboClient;
    private final SupplierCircuitBreakerGateway circuitBreakerGateway;
    private final SupplierContentCacheRepository contentCacheRepository;
    private final SupplierCredentialRepository credentialRepository;
    private final SupplierCredentialAuditLogRepository auditLogRepository;
    private final SupplierSecretsService supplierSecretsService;
    // TODO: inject ByosClient once BYOS search-merge (DMC-08) is built,
    // following the HotelbedsClient/StubaClient/TboClient pattern
    // (PRD Section 10.2.9).
    private final LocalDmcService localDmcService;

    SupplierAggregationService(HotelbedsClient hotelbedsClient, StubaClient stubaClient, TboClient tboClient,
                               SupplierCircuitBreakerGateway circuitBreakerGateway,
                               SupplierContentCacheRepository contentCacheRepository,
                               SupplierCredentialRepository credentialRepository,
                               SupplierCredentialAuditLogRepository auditLogRepository,
                               SupplierSecretsService supplierSecretsService, LocalDmcService localDmcService) {
        this.hotelbedsClient = hotelbedsClient;
        this.stubaClient = stubaClient;
        this.tboClient = tboClient;
        this.circuitBreakerGateway = circuitBreakerGateway;
        this.contentCacheRepository = contentCacheRepository;
        this.credentialRepository = credentialRepository;
        this.auditLogRepository = auditLogRepository;
        this.supplierSecretsService = supplierSecretsService;
        this.localDmcService = localDmcService;
    }

    @Override
    public List<SupplierSearchResult> searchHotels(String locationCode, LocalDate checkIn, LocalDate checkOut) {
        List<CompletableFuture<List<SupplierSearchResult>>> futures = List.of(
            searchAsync(SupplierId.HOTELBEDS, () -> hotelbedsClient.search(locationCode, checkIn, checkOut)),
            searchAsync(SupplierId.STUBA, () -> stubaClient.search(locationCode, checkIn, checkOut)),
            // TBO's TraceId is itinerary-draft-scoped (PRD §10.2.3); passing
            // null here starts a fresh search-session TraceId each call —
            // persisting/reusing it against the draft itinerary is follow-up
            // work once the itinerary-builder search flow calls this method.
            searchAsync(SupplierId.TBO, () -> tboClient.search(locationCode, checkIn, checkOut, null).results())
        );
        // TODO: merge Local DMC/BYOS results here too, then apply
        // deduplication (BOK-20) + the Default Selection Algorithm
        // (PRD Section 9.2, 9.4).
        return futures.stream()
            .map(CompletableFuture::join)
            .flatMap(List::stream)
            .map(this::enrichWithCachedContent)
            .toList();
    }

    /**
     * Fills in {@code rating} from BOK-27's content cache instead of leaving
     * it hard-coded {@code null} — the gap {@code HotelbedsClient}'s stub
     * comment calls out ("real rating requires supplier content sync — not
     * wired for any supplier yet"). A cache miss (not yet synced) leaves the
     * result unchanged; FND-14's Default Selection Algorithm already treats
     * a missing rating as the lowest tiebreak score rather than failing.
     */
    private SupplierSearchResult enrichWithCachedContent(SupplierSearchResult result) {
        if (result.rating() != null) {
            return result;
        }
        return contentCacheRepository.findBySupplierIdAndSupplierContentId(result.supplierId(), result.supplierRateId())
            .map(cache -> new SupplierSearchResult(result.supplierId(), result.supplierRateId(), result.propertyName(),
                result.roomType(), result.netRate(), cache.getRating()))
            .orElse(result);
    }

    private CompletableFuture<List<SupplierSearchResult>> searchAsync(
            SupplierId supplierId, java.util.function.Supplier<List<SupplierSearchResult>> call) {
        return CompletableFuture
            .supplyAsync(() -> circuitBreakerGateway.call(supplierId, call, List.of()))
            .orTimeout(SUPPLIER_CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .exceptionally(ex -> List.of()); // timeout or unexpected failure -> exclude this supplier (PRD §10.2.1)
    }

    @Override
    @Transactional
    public void updateSupplierCredential(UpdateSupplierCredentialCommand command) {
        UUID userId = CurrentPrincipal.get().userId();
        // RULES.md §5.3 — command.secretValue() is used once, to write the
        // real credential to Secrets Manager, and never touched again;
        // only the resulting ARN is persisted in Postgres from here on.
        String secretArn = supplierSecretsService.storeSecret(command.supplierId(), command.secretValue());
        SupplierCredential credential = credentialRepository.findById(command.supplierId())
            .orElseGet(() -> new SupplierCredential(command.supplierId(), secretArn, userId));
        credential.rotate(secretArn, userId);
        credentialRepository.save(credential);

        auditLogRepository.save(new SupplierCredentialAuditLog(UUID.randomUUID(), command.supplierId(), userId));
    }

    @Override
    public List<SupplierCredentialSummary> listSupplierCredentials() {
        return credentialRepository.findAll().stream()
            .map(c -> new SupplierCredentialSummary(c.getSupplierId(), true, c.getLastModifiedByUserId(), c.getLastModifiedAt()))
            .toList();
    }

    @Override
    public UUID submitLocalDmc(SubmitLocalDmcCommand command) {
        return localDmcService.submitLocalDmc(command);
    }

    @Override
    public void activateLocalDmc(UUID localDmcId, ActivateLocalDmcCommand command) {
        localDmcService.activateLocalDmc(localDmcId, command);
    }

    @Override
    public Page<LocalDmcView> findLocalDmcs(UUID consultantId, Pageable pageable) {
        return localDmcService.findLocalDmcs(consultantId, pageable);
    }
}
