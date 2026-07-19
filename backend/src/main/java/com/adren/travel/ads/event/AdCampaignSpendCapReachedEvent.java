package com.adren.travel.ads.event;

import java.math.BigDecimal;
import java.util.UUID;

/** ADS-10, PRD §14.3/§24.6 — a Live campaign's spend reached its budget cap and auto-paused spending. */
public record AdCampaignSpendCapReachedEvent(UUID campaignId, UUID consultantId, BigDecimal spendToDateAmount) {
}
