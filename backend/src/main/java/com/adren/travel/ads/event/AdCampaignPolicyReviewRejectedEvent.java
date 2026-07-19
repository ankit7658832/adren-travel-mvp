package com.adren.travel.ads.event;

import java.util.UUID;

/** ADS-06, PRD §14.2 step 5 — Super Admin rejected a campaign at brand-safety/policy review; {@code reason} is surfaced to the Consultant. */
public record AdCampaignPolicyReviewRejectedEvent(UUID campaignId, UUID consultantId, String reason) {
}
