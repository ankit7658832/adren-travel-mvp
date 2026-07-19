package com.adren.travel.ads.event;

import java.math.BigDecimal;
import java.util.UUID;

/** ADS-03 — a Consultant supplied audience/budget/duration for a campaign (PRD §14.2 steps 1-2). */
public record AdCampaignInputsSubmittedEvent(
    UUID campaignId, String audienceDescription, BigDecimal budgetCapAmount, Integer durationDays) {
}
