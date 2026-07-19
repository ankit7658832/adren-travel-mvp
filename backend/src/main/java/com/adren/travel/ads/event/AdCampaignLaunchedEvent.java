package com.adren.travel.ads.event;

import java.util.UUID;

/** ADS-07, PRD §14.2 step 6 — a campaign went Live under the Adren-managed Meta account. */
public record AdCampaignLaunchedEvent(UUID campaignId, UUID consultantId, String metaCampaignRef) {
}
