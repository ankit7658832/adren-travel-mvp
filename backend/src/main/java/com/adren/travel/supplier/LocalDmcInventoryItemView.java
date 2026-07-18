package com.adren.travel.supplier;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** One Local DMC inventory item (PRD §10.2.8, DMC-03/10/11) — never the JPA entity itself. */
public record LocalDmcInventoryItemView(
    UUID itemId,
    UUID localDmcId,
    String productName,
    String category,
    BigDecimal netRate,
    String netRateCurrency,
    String cancellationPolicyText,
    LocalDate availableFrom,
    LocalDate availableTo,
    Instant updatedAt
) {
}
