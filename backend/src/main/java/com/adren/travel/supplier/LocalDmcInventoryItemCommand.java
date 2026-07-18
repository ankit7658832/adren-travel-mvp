package com.adren.travel.supplier;

import com.adren.travel.shared.CurrencyCode;
import com.adren.travel.shared.ProductCategory;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One Local DMC inventory item's fields (PRD §10.2.8, DMC-03/10) — shared
 * by both a validated CSV row and a single-item create/update, since both
 * carry the exact same required fields.
 */
public record LocalDmcInventoryItemCommand(
    String productName,
    ProductCategory category,
    BigDecimal netRate,
    CurrencyCode netRateCurrency,
    String cancellationPolicyText,
    LocalDate availableFrom,
    LocalDate availableTo
) {
}
