package com.adren.travel.ads.event;

import java.util.UUID;

/** ADS-02 — a new campaign entered PendingApproval (PRD §14.2 step 2, §20.13). */
public record AdCampaignCreatedEvent(UUID campaignId, UUID packageId, UUID consultantId) {
}
