package com.adren.travel.ads.internal;

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
}
