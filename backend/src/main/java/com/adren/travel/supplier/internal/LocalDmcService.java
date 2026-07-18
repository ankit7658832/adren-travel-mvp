package com.adren.travel.supplier.internal;

import com.adren.travel.security.AdrenPrincipal;
import com.adren.travel.security.CurrentPrincipal;
import com.adren.travel.supplier.ActivateLocalDmcCommand;
import com.adren.travel.supplier.LocalDmcInventoryItemCommand;
import com.adren.travel.supplier.LocalDmcInventoryItemView;
import com.adren.travel.supplier.LocalDmcInventoryUploadResult;
import com.adren.travel.supplier.LocalDmcView;
import com.adren.travel.supplier.SubmitLocalDmcCommand;
import com.adren.travel.supplier.internal.localdmc.LocalDmcInventoryItem;
import com.adren.travel.supplier.internal.localdmc.LocalDmcInventoryItemRepository;
import com.adren.travel.supplier.internal.localdmc.LocalDmcRecord;
import com.adren.travel.supplier.internal.localdmc.LocalDmcRecordRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Local DMC onboarding/vetting/quality-signal logic (PRD §10.3, §20.14,
 * DMC-01/02/04/05/10/11) — a separate collaborator {@link
 * SupplierAggregationService} delegates to (matching {@code
 * booking.internal.BookingServiceImpl}'s own use of {@code
 * HotelDedupService} as a helper collaborator) rather than growing
 * {@code SupplierAggregationService}'s own constructor further past
 * backend-best-practices §4's ~4-5-dependency decomposition signal.
 * <p>
 * <b>DMC-04's "cancellation rate recalculates on booking cancellation" is
 * only reachable via {@link #recordCancellation}, a direct method call —
 * NOT a live listener on {@code booking.event.BookingCancelledEvent}.</b>
 * This is a deliberate, flagged scope boundary, not an oversight: Local DMC
 * inventory isn't wired into the real search→booking flow anywhere in this
 * story catalogue (unlike BYOS, which DMC-08 explicitly merges into search
 * results) — {@code HotelLineItem}/{@code BookingCancelledEvent} carry no
 * per-line-item attribution back to a specific Local DMC today, so a
 * listener here could never fire correctly. Building one anyway would be
 * dead code masquerading as real wiring. This method is the correct, real,
 * testable hook a future search-merge story would call once that
 * attribution exists — same shape as every other cross-module integration
 * point in this codebase (caller supplies the structured fact, this module
 * never infers it).
 */
@Component
class LocalDmcService {

    private final LocalDmcRecordRepository repository;
    private final LocalDmcQualityThresholds thresholds;
    private final LocalDmcInventoryItemRepository inventoryRepository;
    private final LocalDmcInventoryCsvParser csvParser;

    LocalDmcService(LocalDmcRecordRepository repository, LocalDmcQualityThresholds thresholds,
                    LocalDmcInventoryItemRepository inventoryRepository, LocalDmcInventoryCsvParser csvParser) {
        this.repository = repository;
        this.thresholds = thresholds;
        this.inventoryRepository = inventoryRepository;
        this.csvParser = csvParser;
    }

    @Transactional
    UUID submitLocalDmc(SubmitLocalDmcCommand command) {
        UUID consultantId = CurrentPrincipal.get().consultantId();
        UUID localDmcId = UUID.randomUUID();
        LocalDmcRecord record = new LocalDmcRecord(localDmcId, consultantId, command.businessName(),
            String.join(",", command.productCategories()), command.sampleRatesSummary(), command.referencesInfo());
        repository.save(record);
        return localDmcId;
    }

    @Transactional
    void activateLocalDmc(UUID localDmcId, ActivateLocalDmcCommand command) {
        LocalDmcRecord record = findOwned(localDmcId);
        record.activate(command.verificationNotes());
        repository.save(record);
    }

    Page<LocalDmcView> findLocalDmcs(UUID consultantId, Pageable pageable) {
        AdrenPrincipal principal = CurrentPrincipal.get();
        // Unlike CurrentPrincipal.resolveTenantScope (which throws on any
        // mismatch), a Consultant here is always silently scoped to their
        // own tenant regardless of what (if anything) they passed — this
        // endpoint is used identically by both roles, and the frontend has
        // no session-derived consultantId to pass yet (no login/session
        // story has landed), so "my own, always" is the only correct
        // behavior for a non-Super-Admin caller.
        UUID scopedConsultantId = principal.isSuperAdmin() ? consultantId : principal.consultantId();
        Page<LocalDmcRecord> page = scopedConsultantId != null
            ? repository.findByConsultantId(scopedConsultantId, pageable)
            : repository.findAll(pageable);
        return page.map(LocalDmcService::toView);
    }

    LocalDmcRecord findOwned(UUID localDmcId) {
        LocalDmcRecord record = repository.findById(localDmcId)
            .orElseThrow(() -> new IllegalArgumentException("No Local DMC: " + localDmcId));
        CurrentPrincipal.resolveTenantScope(record.getConsultantId());
        return record;
    }

    @Transactional
    void recordBooking(UUID localDmcId) {
        LocalDmcRecord record = repository.findById(localDmcId)
            .orElseThrow(() -> new IllegalArgumentException("No Local DMC: " + localDmcId));
        record.recordBooking();
        repository.save(record);
    }

    @Transactional
    void recordCancellation(UUID localDmcId) {
        LocalDmcRecord record = repository.findById(localDmcId)
            .orElseThrow(() -> new IllegalArgumentException("No Local DMC: " + localDmcId));
        record.recordCancellation(thresholds.cancellationRateThreshold());
        repository.save(record);
    }

    @Transactional
    void recordComplaint(UUID localDmcId) {
        LocalDmcRecord record = repository.findById(localDmcId)
            .orElseThrow(() -> new IllegalArgumentException("No Local DMC: " + localDmcId));
        record.recordComplaint(thresholds.complaintCountThreshold());
        repository.save(record);
    }

    List<LocalDmcRecord> findAllActive() {
        return repository.findByStatus(com.adren.travel.supplier.internal.localdmc.LocalDmcStatus.ACTIVE);
    }

    /**
     * DMC-03: all-or-nothing — a single invalid row rejects the WHOLE
     * upload with every row's field errors; only a fully-clean CSV
     * persists anything, per the story's own "not a partial silent
     * import" AC.
     */
    @Transactional
    LocalDmcInventoryUploadResult bulkUploadLocalDmcInventory(UUID localDmcId, String csvContent) {
        findOwned(localDmcId);
        LocalDmcInventoryCsvParser.ParseResult parsed = csvParser.parse(csvContent);
        if (!parsed.errors().isEmpty()) {
            return new LocalDmcInventoryUploadResult(0, parsed.errors());
        }
        for (LocalDmcInventoryItemCommand row : parsed.validRows()) {
            inventoryRepository.save(new LocalDmcInventoryItem(UUID.randomUUID(), localDmcId, row.productName(),
                row.category(), row.netRate(), row.netRateCurrency(), row.cancellationPolicyText(),
                row.availableFrom(), row.availableTo()));
        }
        return new LocalDmcInventoryUploadResult(parsed.validRows().size(), List.of());
    }

    Page<LocalDmcInventoryItemView> findLocalDmcInventory(UUID localDmcId, Pageable pageable) {
        findOwned(localDmcId);
        return inventoryRepository.findByLocalDmcId(localDmcId, pageable).map(LocalDmcService::toInventoryView);
    }

    private static LocalDmcInventoryItemView toInventoryView(LocalDmcInventoryItem item) {
        return new LocalDmcInventoryItemView(item.getItemId(), item.getLocalDmcId(), item.getProductName(),
            item.getCategory().name(), item.getNetRate(), item.getNetRateCurrency().name(),
            item.getCancellationPolicyText(), item.getAvailableFrom(), item.getAvailableTo(), item.getUpdatedAt());
    }

    private static LocalDmcView toView(LocalDmcRecord record) {
        return new LocalDmcView(record.getLocalDmcId(), record.getConsultantId(), record.getBusinessName(),
            List.of(record.getProductCategories().split(",")), record.getSampleRatesSummary(),
            record.getReferencesInfo(), record.getStatus().name(), record.getVerificationNotes(),
            record.getCancellationRate(), record.getComplaintCount(), record.isFlagged(), record.isInventoryStale(),
            record.getCreatedAt());
    }
}
