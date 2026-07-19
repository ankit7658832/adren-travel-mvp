package com.adren.travel.ads.event;

import java.util.UUID;

/** ADS-05, PRD §14.2 step 4 — a Consultant approved one creative variant; a campaign can only submit for review once every variant carries one of these. */
public record AdCampaignCreativeVariantApprovedEvent(UUID campaignId, UUID variantId) {
}
