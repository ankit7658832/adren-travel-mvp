package com.adren.travel.supplier.internal;

import com.adren.travel.security.CurrentPrincipal;
import com.adren.travel.supplier.SupplierCredentialSummary;
import com.adren.travel.supplier.SupplierSearchApi;
import com.adren.travel.supplier.SupplierSearchResult;
import com.adren.travel.supplier.UpdateSupplierCredentialCommand;
import com.adren.travel.supplier.internal.hotelbeds.HotelbedsClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Aggregates all connected suppliers behind {@link SupplierSearchApi}.
 * Per PRD Section 24.2 (NFR): each supplier call is isolated so one
 * supplier's downtime doesn't degrade the others — in the full
 * implementation, wrap each call below in its own circuit breaker
 * (e.g., Resilience4j) rather than calling them inline as shown in this
 * scaffold.
 */
@Service
class SupplierAggregationService implements SupplierSearchApi {

    private final HotelbedsClient hotelbedsClient;
    private final SupplierCredentialRepository credentialRepository;
    private final SupplierCredentialAuditLogRepository auditLogRepository;
    private final SupplierSecretsService supplierSecretsService;
    // TODO: inject StubaClient, TboClient, LocalDmcRepository, ByosClient
    // as each is built out, following the HotelbedsClient pattern
    // (PRD Section 10.2.2 - 10.2.9).

    SupplierAggregationService(HotelbedsClient hotelbedsClient, SupplierCredentialRepository credentialRepository,
                               SupplierCredentialAuditLogRepository auditLogRepository,
                               SupplierSecretsService supplierSecretsService) {
        this.hotelbedsClient = hotelbedsClient;
        this.credentialRepository = credentialRepository;
        this.auditLogRepository = auditLogRepository;
        this.supplierSecretsService = supplierSecretsService;
    }

    @Override
    public List<SupplierSearchResult> searchHotels(String locationCode, LocalDate checkIn, LocalDate checkOut) {
        List<SupplierSearchResult> results = new ArrayList<>();
        try {
            results.addAll(hotelbedsClient.search(locationCode, checkIn, checkOut));
        } catch (Exception e) {
            // PRD 10.2.1: on timeout, exclude Hotelbeds from this cycle rather
            // than failing the whole search.
        }
        // TODO: merge STUBA/TBO/Local DMC/BYOS results here, then apply the
        // deduplication + Default Selection Algorithm (PRD Section 9.2, 9.4).
        return results;
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
}
