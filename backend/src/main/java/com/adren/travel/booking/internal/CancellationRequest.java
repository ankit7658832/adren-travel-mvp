package com.adren.travel.booking.internal;

import com.adren.travel.shared.CurrencyCode;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Tracks one cancellation through FIN-16/PRD §12.5's policy-check →
 * refund/penalty calculation → approval (if a penalty applies) → refund-
 * processed workflow — package-private, own table. The calculation itself
 * (refund/penalty split, FX conversion) is done once up front by {@code
 * PaymentsApi.calculateRefund} (FIN-13/14) and stored here; this entity's
 * job is only the state machine sitting on top of that calculation, not
 * re-deriving it.
 */
@Entity
@Table(name = "cancellation_request")
class CancellationRequest {

    @Id
    private UUID cancellationRequestId;

    private UUID bookingId;
    private UUID consultantId;

    @Column(name = "refund_amount")
    private BigDecimal refundAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "refund_currency")
    private CurrencyCode refundCurrency;

    @Column(name = "penalty_amount")
    private BigDecimal penaltyAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "penalty_currency")
    private CurrencyCode penaltyCurrency;

    @Enumerated(EnumType.STRING)
    private CancellationStatus status;

    private Instant createdAt;
    private Instant approvedAt;
    private Instant refundedAt;

    protected CancellationRequest() {
        // JPA
    }

    /**
     * Submits a cancellation (PRD §12.5, FIN-16) — {@code requiresApproval}
     * comes straight from {@code RefundCalculation.requiresConsultantApproval()}:
     * a penalty applies, so this starts life {@code PENDING_APPROVAL}; a
     * penalty-free refund has nothing for a Consultant to decide, so it
     * starts already {@code APPROVED} (ready for the caller to process the
     * refund in the same transaction, no separate approval step needed).
     */
    static CancellationRequest submit(UUID cancellationRequestId, UUID bookingId, UUID consultantId,
                                       BigDecimal refundAmount, CurrencyCode refundCurrency,
                                       BigDecimal penaltyAmount, CurrencyCode penaltyCurrency,
                                       boolean requiresApproval) {
        CancellationRequest request = new CancellationRequest();
        request.cancellationRequestId = cancellationRequestId;
        request.bookingId = bookingId;
        request.consultantId = consultantId;
        request.refundAmount = refundAmount;
        request.refundCurrency = refundCurrency;
        request.penaltyAmount = penaltyAmount;
        request.penaltyCurrency = penaltyCurrency;
        request.createdAt = Instant.now();
        if (requiresApproval) {
            request.status = CancellationStatus.PENDING_APPROVAL;
        } else {
            request.status = CancellationStatus.APPROVED;
            request.approvedAt = request.createdAt;
        }
        return request;
    }

    /**
     * A Consultant explicitly approves a penalized cancellation (PRD
     * §12.5's AC: "pauses for explicit Consultant approval before the
     * refund is processed"). Throws rather than silently no-op'ing on an
     * already-approved/refunded request — see backend-best-practices §1.
     */
    void approve() {
        if (status != CancellationStatus.PENDING_APPROVAL) {
            throw new IllegalStateException(
                "Cannot approve cancellation %s: status is %s, expected PENDING_APPROVAL"
                    .formatted(cancellationRequestId, status));
        }
        this.status = CancellationStatus.APPROVED;
        this.approvedAt = Instant.now();
    }

    /** The refund has actually been credited back to the wallet (FIN-16) — only reachable once approved. */
    void markRefunded() {
        if (status != CancellationStatus.APPROVED) {
            throw new IllegalStateException(
                "Cannot mark cancellation %s refunded: status is %s, expected APPROVED"
                    .formatted(cancellationRequestId, status));
        }
        this.status = CancellationStatus.REFUNDED;
        this.refundedAt = Instant.now();
    }

    UUID getCancellationRequestId() {
        return cancellationRequestId;
    }

    UUID getBookingId() {
        return bookingId;
    }

    UUID getConsultantId() {
        return consultantId;
    }

    BigDecimal getRefundAmount() {
        return refundAmount;
    }

    CurrencyCode getRefundCurrency() {
        return refundCurrency;
    }

    BigDecimal getPenaltyAmount() {
        return penaltyAmount;
    }

    CurrencyCode getPenaltyCurrency() {
        return penaltyCurrency;
    }

    CancellationStatus getStatus() {
        return status;
    }
}
