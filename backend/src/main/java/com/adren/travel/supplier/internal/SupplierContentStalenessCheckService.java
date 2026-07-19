package com.adren.travel.supplier.internal;

import com.adren.travel.supplier.SupplierId;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

/**
 * Scheduled staleness check over live-supplier content (PRD §10.5, HRD-13) —
 * complements {@link LocalDmcInventoryStalenessCheckService} (DMC-11), which
 * covers the manual/no-live-API path; this covers the suppliers {@link
 * SupplierContentSyncService} actually syncs. Flips {@link
 * SupplierContentCache#isStale()} on/off each run — the persisted,
 * queryable signal a future Super Admin alert screen would surface, since
 * (as DMC-11's own Javadoc already established) no Super Admin-addressable
 * notification channel exists anywhere in this codebase — {@code
 * notification} is entirely per-Consultant addressed.
 */
@Service
class SupplierContentStalenessCheckService {

    private final SupplierContentCacheRepository repository;
    private final SupplierContentStalenessThresholds thresholds;

    SupplierContentStalenessCheckService(
        SupplierContentCacheRepository repository, SupplierContentStalenessThresholds thresholds) {
        this.repository = repository;
        this.thresholds = thresholds;
    }

    /**
     * Independent of {@link SupplierContentSyncService}'s own sync cadences
     * so a wedged/failing sync job is itself detectable — running the check
     * on the same cadence as the thing it's checking would let a sync job
     * that silently stopped firing go unnoticed forever.
     */
    @Scheduled(cron = "${adren.supplier.content-sync.staleness-check-cron:0 0 5 * * *}")
    @Transactional
    void checkStaleness() {
        for (SupplierContentCache cache : repository.findAll()) {
            boolean stale = isStale(cache.getSupplierId(), cache.getLastSyncedAt());
            if (stale != cache.isStale()) {
                cache.setStale(stale);
                repository.save(cache);
            }
        }
    }

    private boolean isStale(SupplierId supplierId, Instant lastSyncedAt) {
        return lastSyncedAt.isBefore(Instant.now().minus(thresholdFor(supplierId)));
    }

    /**
     * @throws IllegalArgumentException for a supplier {@link
     *         SupplierContentSyncService} never syncs (MYSTIFLY, TRANSFERZ —
     *         no static-content analogue per §10.2.4/§10.2.5 — or LOCAL_DMC/
     *         BYOS, DMC-11's territory) — {@link #checkStaleness} only ever
     *         iterates rows this same module wrote, so such a row would
     *         indicate a real bug, not a legitimate case to silently skip.
     */
    private Duration thresholdFor(SupplierId supplierId) {
        return switch (supplierId) {
            case HOTELBEDS, STUBA, TBO -> thresholds.hotelsStalenessThreshold();
            case WIDGETY -> thresholds.cruiseStalenessThreshold();
            case HBACTIVITIES -> thresholds.activitiesStalenessThreshold();
            case MYSTIFLY, TRANSFERZ, LOCAL_DMC, BYOS ->
                throw new IllegalArgumentException("Not a content-synced supplier: " + supplierId);
        };
    }
}
