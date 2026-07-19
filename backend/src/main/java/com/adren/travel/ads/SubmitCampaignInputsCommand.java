package com.adren.travel.ads;

import java.math.BigDecimal;
import java.util.UUID;

/** ADS-03 — audience/budget/duration inputs for an existing campaign, PRD §14.2 steps 1-2. */
public record SubmitCampaignInputsCommand(
    UUID campaignId, String audienceDescription, BigDecimal budgetCapAmount, Integer durationDays) {
}
