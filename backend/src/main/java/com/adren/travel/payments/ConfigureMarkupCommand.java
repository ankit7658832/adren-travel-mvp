package com.adren.travel.payments;

import com.adren.travel.shared.CurrencyCode;
import com.adren.travel.shared.ProductCategory;

import java.math.BigDecimal;

/**
 * Cross-module-safe input to {@link PaymentsApi#configureMarkup} (PRD
 * §12.1, FIN-01) — a plain value, never a JPA entity (RULES.md §1.4).
 * Exactly one of {@code percentageValue} or ({@code flatFeeAmount},
 * {@code flatFeeCurrency}) is populated, matching {@code markupType}.
 */
public record ConfigureMarkupCommand(
    ProductCategory category,
    MarkupType markupType,
    BigDecimal percentageValue,
    BigDecimal flatFeeAmount,
    CurrencyCode flatFeeCurrency
) {
}
