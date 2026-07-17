package com.adren.travel.payments;

import com.adren.travel.shared.CurrencyCode;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * One row of a Consultant's wallet transaction ledger (PRD §20.12/§21.7,
 * FIN-09) — never the JPA entity itself. {@code type} is one of {@code
 * TOP_UP}, {@code HOLD}, {@code DEBIT}, {@code REFUND}, {@code
 * COMMISSION_DEDUCTION}, {@code RELEASE}, {@code ON_ACCOUNT} as a plain
 * {@code String} rather than an exposed enum type, since {@code
 * LedgerEntryType} is an {@code internal} implementation detail (RULES.md
 * §4.1) — the same shape {@code CancellationRequestView.status} uses.
 */
public record WalletLedgerEntryView(
    UUID ledgerEntryId,
    UUID consultantId,
    String type,
    BigDecimal amount,
    CurrencyCode currency,
    UUID relatedBookingId,
    BigDecimal balanceAfter,
    Instant createdAt
) {
}
