package com.adren.travel.payments;

import com.adren.travel.shared.CurrencyCode;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * A Consultant's wallet snapshot (PRD §12.3/§20.12, FIN-06) — cross-module-safe,
 * never the JPA entity itself. {@code availableBalance}, {@code creditLimit}
 * and {@code pendingHolds} are all denominated in {@code currency}, the
 * Consultant's home-market settlement currency.
 */
public record WalletView(
    UUID consultantId,
    BigDecimal availableBalance,
    BigDecimal creditLimit,
    BigDecimal pendingHolds,
    CurrencyCode currency,
    Instant updatedAt
) {
}
