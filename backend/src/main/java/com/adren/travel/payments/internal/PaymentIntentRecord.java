package com.adren.travel.payments.internal;

import com.adren.travel.payments.PaymentIntentStatus;
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
 * The persisted mapping a Stripe webhook needs to find its way back to a
 * booking or wallet top-up (PRD §12.4, FIN-11; §23.4 Edge Case #10, FIN-15)
 * — package-private, own table, keyed by Stripe's own PaymentIntent id
 * since that's the only identifier the webhook payload carries.
 * {@code bookingReferenceId} is a synthetic reference (not a real booking)
 * for {@code WALLET_TOP_UP}-purpose records — there is no booking to
 * reference for a top-up.
 */
@Entity
@Table(name = "payment_intent")
class PaymentIntentRecord {

    @Id
    private String paymentIntentId;

    private UUID bookingReferenceId;
    private UUID consultantId;
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private CurrencyCode currency;

    @Enumerated(EnumType.STRING)
    private PaymentIntentStatus status;

    @Enumerated(EnumType.STRING)
    private PaymentIntentPurpose purpose;

    private Instant createdAt;
    private Instant updatedAt;

    protected PaymentIntentRecord() {
        // JPA
    }

    PaymentIntentRecord(String paymentIntentId, UUID bookingReferenceId, UUID consultantId, BigDecimal amount,
                         CurrencyCode currency, PaymentIntentStatus status, PaymentIntentPurpose purpose) {
        this.paymentIntentId = paymentIntentId;
        this.bookingReferenceId = bookingReferenceId;
        this.consultantId = consultantId;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
        this.purpose = purpose;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    void markAs(PaymentIntentStatus status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }

    String getPaymentIntentId() {
        return paymentIntentId;
    }

    UUID getBookingReferenceId() {
        return bookingReferenceId;
    }

    UUID getConsultantId() {
        return consultantId;
    }

    BigDecimal getAmount() {
        return amount;
    }

    CurrencyCode getCurrency() {
        return currency;
    }

    PaymentIntentStatus getStatus() {
        return status;
    }

    PaymentIntentPurpose getPurpose() {
        return purpose;
    }
}
