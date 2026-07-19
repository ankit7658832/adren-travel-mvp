package com.adren.travel.ads.event;

import java.util.UUID;

/** ADS-06, PRD §14.2 step 5 — a Consultant-approved campaign entered the Super Admin's review queue. */
public record AdCampaignSubmittedForPolicyReviewEvent(UUID campaignId, UUID consultantId) {
}
