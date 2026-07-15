package com.adren.travel.payments.internal;

import com.adren.travel.shared.CurrencyCode;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * A single wallet transaction record (PRD §20.12, FIN-10) —
 * package-private, own table, insert-only (never updated in place —
 * {@code balanceAfter} is a point-in-time snapshot for the audit trail,
 * matching RULES.md §4.4). {@code relatedBookingId} is nullable (a top-up
 * has none); the unique constraint on {@code (relatedBookingId, type)}
 * (see the migration) is what makes a hold/debit/release write idempotent
 * — Postgres treats NULLs as distinct for uniqueness, so non-booking
 * entries (top-ups) never collide with each other under this constraint.
 */
@Entity
@Table(name = "wallet_ledger_entry")
class WalletLedgerEntry {

    @Id
    private UUID ledgerEntryId;

    private UUID consultantId;

    @Enumerated(EnumType.STRING)
    private LedgerEntryType type;

    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private CurrencyCode currency;

    private UUID relatedBookingId;
    private BigDecimal balanceAfter;
    private Instant createdAt;

    protected WalletLedgerEntry() {
        // JPA
    }

    WalletLedgerEntry(UUID ledgerEntryId, UUID consultantId, LedgerEntryType type, BigDecimal amount,
                       CurrencyCode currency, UUID relatedBookingId, BigDecimal balanceAfter) {
        this.ledgerEntryId = ledgerEntryId;
        this.consultantId = consultantId;
        this.type = type;
        this.amount = amount;
        this.currency = currency;
        this.relatedBookingId = relatedBookingId;
        this.balanceAfter = balanceAfter;
        this.createdAt = Instant.now();
    }

    UUID getLedgerEntryId() {
        return ledgerEntryId;
    }

    UUID getConsultantId() {
        return consultantId;
    }

    LedgerEntryType getType() {
        return type;
    }

    BigDecimal getAmount() {
        return amount;
    }

    CurrencyCode getCurrency() {
        return currency;
    }

    UUID getRelatedBookingId() {
        return relatedBookingId;
    }

    BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    Instant getCreatedAt() {
        return createdAt;
    }
}
