package com.adren.travel.shared;

import java.math.BigDecimal;

/**
 * A currency-tagged amount with no single owning consultant — used where a
 * platform-scope aggregate (PRD §21.6's "all-Consultant GMV", "ad spend
 * across Consultants") sums across tenants whose settlement currencies
 * differ, so the result must be a per-currency breakdown, never a single
 * summed {@link Money} (RULES.md §4.4: an amount is never auditable
 * without its currency, and summing across currencies is not a valid
 * operation at all).
 */
public record CurrencyAmount(CurrencyCode currency, BigDecimal amount) {
}
