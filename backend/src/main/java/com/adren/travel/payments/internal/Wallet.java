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
 * {@code pendingHolds} are always denominated in {@code currency}.
 * {@code placeHold}/{@code resolveHoldAsDebit}/{@code resolveHoldAsRelease}
 * (FIN-07) are the hold lifecycle; the audit trail those write to lives in
 * {@link WalletLedgerEntry} (FIN-10).
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

    /** A booking reaching the payment step sets aside funds (PRD §12.3, FIN-07) — increases pendingHolds only. */
    void placeHold(BigDecimal amount) {
        this.pendingHolds = this.pendingHolds.add(amount);
        this.updatedAt = Instant.now();
    }

    /** The booking confirms (PRD §12.3, FIN-07) — the hold becomes an actual charge: pendingHolds and availableBalance both decrease. */
    void resolveHoldAsDebit(BigDecimal amount) {
        this.pendingHolds = this.pendingHolds.subtract(amount);
        this.availableBalance = this.availableBalance.subtract(amount);
        this.updatedAt = Instant.now();
    }

    /** The booking is abandoned/cancelled before confirmation (PRD §12.3, FIN-07) — the hold is released, availableBalance untouched. */
    void resolveHoldAsRelease(BigDecimal amount) {
        this.pendingHolds = this.pendingHolds.subtract(amount);
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
