package com.adren.travel.ads;

import com.adren.travel.shared.CurrencyCode;

import java.math.BigDecimal;
import java.util.UUID;

/** A campaign's current state (PRD §20.13) — cross-module-safe, never the JPA entity itself. */
public record AdCampaignView(
    UUID campaignId,
    UUID packageId,
    UUID consultantId,
    String status,
    String audienceDescription,
    BigDecimal budgetCapAmount,
    CurrencyCode budgetCapCurrency,
    Integer durationDays,
    String metaCampaignRef,
    BigDecimal spendToDateAmount,
    String rejectionReason,
    int impressions,
    int clicks,
    int bookingsAttributed
) {
}
