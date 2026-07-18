package com.adren.travel.supplier.internal;

import com.adren.travel.supplier.internal.localdmc.LocalDmcInventoryItemRepository;
import com.adren.travel.supplier.internal.localdmc.LocalDmcRecord;
import com.adren.travel.supplier.internal.localdmc.LocalDmcRecordRepository;
import com.adren.travel.supplier.internal.localdmc.LocalDmcStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Scheduled staleness check over Local DMC inventory (PRD §10.5, DMC-11) —
 * the manual/no-live-API equivalent of {@link SupplierContentSyncService}'s
 * sync cadence, since a Local DMC's inventory has no automatic sync that
 * could otherwise fail and trigger the alerting §10.5 already requires for
 * the live-API suppliers. Flips {@link LocalDmcRecord#isInventoryStale()}
 * on/off each run — the persisted, queryable signal a future Super Admin
 * alert screen would surface (same "query method, not a literal push"
 * shape as {@link SupplierContentSyncService#isStale}, since no Super
 * Admin-addressable notification channel exists anywhere in this
 * codebase — {@code notification} is entirely per-Consultant addressed).
 */
@Service
class LocalDmcInventoryStalenessCheckService {

    private final LocalDmcRecordRepository recordRepository;
    private final LocalDmcInventoryItemRepository inventoryRepository;
    private final LocalDmcQualityThresholds thresholds;

    LocalDmcInventoryStalenessCheckService(LocalDmcRecordRepository recordRepository,
                                           LocalDmcInventoryItemRepository inventoryRepository,
                                           LocalDmcQualityThresholds thresholds) {
        this.recordRepository = recordRepository;
        this.inventoryRepository = inventoryRepository;
        this.thresholds = thresholds;
    }

    /** Nightly, mirroring {@link SupplierContentSyncService}'s hotel/activity cadence. */
    @Scheduled(cron = "${adren.supplier.local-dmc.staleness-check-cron:0 0 4 * * *}")
    @Transactional
    void checkInventoryStaleness() {
        List<LocalDmcRecord> activeDmcs = recordRepository.findByStatus(LocalDmcStatus.ACTIVE);
        for (LocalDmcRecord record : activeDmcs) {
            boolean stale = isStale(record.getLocalDmcId());
            if (stale != record.isInventoryStale()) {
                record.setInventoryStale(stale);
                recordRepository.save(record);
            }
        }
    }

    /**
     * @return true if this Local DMC has no inventory items at all, or its
     *         most-recently-edited item is older than the configured
     *         staleness threshold — same "never synced == stale" default as
     *         {@link SupplierContentSyncService#isStale}.
     */
    boolean isStale(java.util.UUID localDmcId) {
        Instant threshold = Instant.now().minus(Duration.ofDays(thresholds.inventoryStalenessThresholdDays()));
        return inventoryRepository.findTopByLocalDmcIdOrderByUpdatedAtDesc(localDmcId)
            .map(item -> item.getUpdatedAt().isBefore(threshold))
            .orElse(true);
    }
}
