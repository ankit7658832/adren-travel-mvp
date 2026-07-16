package com.adren.travel.payments.internal;

import com.adren.travel.payments.CreditLimitExceededException;
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

    /**
     * A booking reaching the payment step sets aside funds (PRD §12.3,
     * FIN-07) — increases pendingHolds only. PRD §22.4 T8/FIN-08: rejected
     * outright if it would push {@code pendingHolds} past {@code
     * availableBalance + creditLimit} — this in-memory check is the
     * fast-path/UX layer (an actionable message before any DB round trip);
     * the {@code chk_wallet_within_credit_limit} CHECK constraint (V29) is
     * the actual DB-level guarantee backend-best-practices §3 calls for,
     * catching what a same-wallet concurrent-hold race could otherwise slip
     * past this single-threaded in-memory check.
     */
    void placeHold(BigDecimal amount) {
        BigDecimal newPendingHolds = this.pendingHolds.add(amount);
        if (availableBalance.add(creditLimit).subtract(newPendingHolds).signum() < 0) {
            throw new CreditLimitExceededException(consultantId);
        }
        this.pendingHolds = newPendingHolds;
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

    /**
     * Increases {@code availableBalance} — the entity-level primitive a
     * wallet top-up (FIN-15, on Stripe webhook success) or on-account
     * billing settlement (FIN-12) will call once those flows exist; not
     * wired to any REST endpoint yet. Added now because FIN-08's credit
     * check makes a wallet's funding path a genuine prerequisite rather
     * than a theoretical one — every wallet was reachable at zero balance
     * before this, which made the credit-limit check untestable/always-tripped.
     */
    void credit(BigDecimal amount) {
        this.availableBalance = this.availableBalance.add(amount);
        this.updatedAt = Instant.now();
    }

    /**
     * Sets {@code creditLimit} — the entity-level primitive a future Super
     * Admin/Consultant credit-grant action will call; not wired to any REST
     * endpoint yet (no such story exists in the mvp-mock catalogue today).
     */
    void grantCreditLimit(BigDecimal newLimit) {
        this.creditLimit = newLimit;
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
