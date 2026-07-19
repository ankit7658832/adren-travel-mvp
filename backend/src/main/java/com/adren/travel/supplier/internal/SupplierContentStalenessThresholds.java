package com.adren.travel.supplier.internal;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Binds {@code adren.supplier.content-sync.*-staleness-threshold} from
 * application.yml (HRD-13) — one threshold per sync cadence category
 * (hotels/activities are nightly, cruise is weekly per {@link
 * SupplierContentSyncService}'s own cadences), since a single shared
 * threshold would either falsely flag weekly-synced cruise content as
 * stale after two days, or let a broken nightly hotel sync run silently
 * for a week before anyone noticed.
 */
@ConfigurationProperties(prefix = "adren.supplier.content-sync")
record SupplierContentStalenessThresholds(
    Duration hotelsStalenessThreshold, Duration cruiseStalenessThreshold, Duration activitiesStalenessThreshold) {
}
