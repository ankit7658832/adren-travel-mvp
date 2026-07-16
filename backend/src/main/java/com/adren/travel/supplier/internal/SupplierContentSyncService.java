package com.adren.travel.supplier.internal;

import com.adren.travel.supplier.SupplierId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

/**
 * Scheduled static-content sync/cache (PRD §10.5, BOK-27) — pulls each
 * supplier's descriptive content (name, rating, photos as applicable) on
 * its own cadence and writes it to {@link SupplierContentCache}, so search
 * results can be enriched with real content instead of a hard-coded
 * {@code null} rating (the gap {@code HotelbedsClient}'s stub comment
 * already calls out).
 * <p>
 * Cadences match PRD §10.2's differing sync-frequency notes: nightly for
 * hotels (§10.2.1 Hotelbeds' "nightly batch job", extended here to
 * STUBA/TBO) and activities (§10.2.7), weekly for cruise (§10.2.6 — "lower
 * change-frequency than hotel content"). Transferz/Mystifly are excluded —
 * §10.2.4/§10.2.5 note transfers and flights have no meaningful static-
 * content analogue. HRD-12 makes these cadences operator-tunable via
 * config on top of this baseline.
 */
@Service
class SupplierContentSyncService {

    private static final Logger log = LoggerFactory.getLogger(SupplierContentSyncService.class);

    private final SupplierContentCacheRepository repository;

    SupplierContentSyncService(SupplierContentCacheRepository repository) {
        this.repository = repository;
    }

    /** Nightly, per §10.2.1/10.2.2/10.2.3 (Hotelbeds/STUBA/TBO). */
    @Scheduled(cron = "${adren.supplier.content-sync.hotels-cron:0 0 2 * * *}")
    @Transactional
    void syncHotelContent() {
        // TODO: real per-supplier Content API calls. Stub content IDs match
        // each client's current stub supplierRateId so search-time
        // enrichment (SupplierContentEnrichmentService) has something to
        // match against until real supplier calls replace both stubs.
        refresh(SupplierId.HOTELBEDS, "stub-rate-key", "Stub Hotel — replace with live Hotelbeds response", 4.2);
        refresh(SupplierId.STUBA, "stub-offer-id", "Stub STUBA Hotel — replace with live STUBA response", 4.0);
        refresh(SupplierId.TBO, "stub-result-index", "Stub TBO Hotel — replace with live TBO response", 3.8);
    }

    /** Weekly, per §10.2.6 (Widgety) — lower change-frequency than hotel content. */
    @Scheduled(cron = "${adren.supplier.content-sync.cruise-cron:0 0 3 * * MON}")
    @Transactional
    void syncCruiseContent() {
        refresh(SupplierId.WIDGETY, "stub-sailing-id", "Stub Cruise Line — replace with live Widgety response", 4.5);
    }

    /** Nightly, per §10.2.7 (HBActivities). */
    @Scheduled(cron = "${adren.supplier.content-sync.activities-cron:0 0 2 * * *}")
    @Transactional
    void syncActivityContent() {
        refresh(SupplierId.HBACTIVITIES, "stub-activity-id", "Stub Activity — replace with live HBActivities response", 4.1);
    }

    private void refresh(SupplierId supplierId, String supplierContentId, String name, double rating) {
        SupplierContentCache cache = repository.findBySupplierIdAndSupplierContentId(supplierId, supplierContentId)
            .orElseGet(() -> new SupplierContentCache(supplierId, supplierContentId));
        cache.refresh(name, rating);
        repository.save(cache);
    }

    /**
     * @return true if no sync has ever run for this content, or the last
     *         sync is older than {@code staleThreshold} — the signal HRD-13's
     *         Super Admin alert is built on top of.
     */
    boolean isStale(SupplierId supplierId, String supplierContentId, Duration staleThreshold) {
        return repository.findBySupplierIdAndSupplierContentId(supplierId, supplierContentId)
            .map(cache -> cache.getLastSyncedAt().isBefore(Instant.now().minus(staleThreshold)))
            .orElse(true);
    }
}
