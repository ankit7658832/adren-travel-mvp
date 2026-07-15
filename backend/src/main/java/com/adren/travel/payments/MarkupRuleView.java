package com.adren.travel.payments;

import com.adren.travel.shared.CurrencyCode;
import com.adren.travel.shared.ProductCategory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** A Consultant's current markup rule for one category (PRD §12.1, FIN-01) — cross-module-safe, never the JPA entity itself. */
public record MarkupRuleView(
    UUID consultantId,
    ProductCategory category,
    MarkupType markupType,
    BigDecimal percentageValue,
    BigDecimal flatFeeAmount,
    CurrencyCode flatFeeCurrency,
    Instant updatedAt
) {
}
