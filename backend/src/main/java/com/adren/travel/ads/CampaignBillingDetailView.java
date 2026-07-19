package com.adren.travel.ads;

import com.adren.travel.shared.CurrencyCode;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * PRD §14.3's billing-transparency AC: spend-to-date, budget cap, and a
 * per-transaction breakdown all visible together, not summarized into one
 * opaque figure (ADS-11) — cross-module-safe, never the JPA entity itself.
 */
public record CampaignBillingDetailView(
    UUID campaignId,
    BigDecimal spendToDateAmount,
    BigDecimal budgetCapAmount,
    CurrencyCode budgetCapCurrency,
    List<SpendTransactionView> transactions
) {
}
