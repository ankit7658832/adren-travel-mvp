package com.adren.travel.ads.internal;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Seam over the Meta Marketing API (PRD §14.1) — this mock phase ships
 * only {@link StubMetaAdsClient}; the real integration is Phase 2's MADS
 * epic, swapped in behind this same interface (same "stub now, real
 * provider later" shape as {@code notification.internal.EmailClient}).
 */
interface MetaAdsClient {

    /** ADS-01 — provisions an Adren-managed Business Manager for a Consultant, returning Meta's account identifier. */
    String provisionAdAccount(UUID consultantId);

    /** ADS-07 — launches a policy-reviewed campaign, returning Meta's campaign identifier. */
    String launchCampaign(UUID campaignId);

    /**
     * ADS-09 — one poll of Meta Insights for a Live campaign: the
     * impressions/clicks/bookings-attributed accrued since the last poll
     * (an increment, not a running total — {@code AdCampaign} owns the
     * running total).
     */
    PerformanceIncrement fetchPerformanceIncrement(UUID campaignId);

    /** ADS-09's increment payload. */
    record PerformanceIncrement(int impressions, int clicks, int bookingsAttributed) {
    }

    /**
     * ADS-10 — one poll of Meta's spend feed for a Live campaign: the
     * amount spent since the last poll (an increment, same shape as
     * {@link #fetchPerformanceIncrement}). Polled on its own, tighter
     * cadence than the performance feed (PRD §24.6's near-real-time NFR
     * applies to spend, not to impressions/clicks display).
     */
    BigDecimal fetchSpendIncrement(UUID campaignId);
}
