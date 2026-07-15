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
 * A Consultant's wallet (PRD §12.3, data dictionary §20.12, FIN-06) —
 * package-private, own table. One row per Consultant, keyed directly by
 * {@code consultantId} rather than a synthetic id, since exactly one wallet
 * exists per tenant. {@code availableBalance}, {@code creditLimit} and
 * {@code pendingHolds} are always denominated in {@code currency} — this
 * story only models the balances; holds/debits (FIN-07) and the ledger
 * (FIN-10) are separate, later stories.
 */
@Entity
@Table(name = "wallet")
class Wallet {

    @Id
    private UUID consultantId;

    private BigDecimal availableBalance;
    private BigDecimal creditLimit;
    private BigDecimal pendingHolds;

    @Enumerated(EnumType.STRING)
    private CurrencyCode currency;

    private Instant updatedAt;

    protected Wallet() {
        // JPA
    }

    /** Newly provisioned wallets start at zero balance, zero credit limit, and zero pending holds. */
    Wallet(UUID consultantId, CurrencyCode currency) {
        this.consultantId = consultantId;
        this.currency = currency;
        this.availableBalance = BigDecimal.ZERO;
        this.creditLimit = BigDecimal.ZERO;
        this.pendingHolds = BigDecimal.ZERO;
        this.updatedAt = Instant.now();
    }

    UUID getConsultantId() {
        return consultantId;
    }

    BigDecimal getAvailableBalance() {
        return availableBalance;
    }

    BigDecimal getCreditLimit() {
        return creditLimit;
    }

    BigDecimal getPendingHolds() {
        return pendingHolds;
    }

    CurrencyCode getCurrency() {
        return currency;
    }

    Instant getUpdatedAt() {
        return updatedAt;
    }
}
