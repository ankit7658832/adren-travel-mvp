package com.adren.travel.booking;

import com.adren.travel.payments.FxRateSnapshot;
import com.adren.travel.shared.Money;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * Inputs to {@link BookingApi#calculateCancellationRefund} (PRD §12.4/§12.5,
 * FIN-13). {@code cancellationDeadline} is the cancelled line item's own
 * field (PRD §20.2, BOK-03) — supplied by the caller since no story has
 * built a Booking-to-line-item linkage yet (see {@code
 * com.adren.travel.payments.CalculateRefundCommand}'s Javadoc for the full
 * scoping note this mirrors). {@code originalFxRateSnapshot} is likewise
 * caller-supplied — this module has no linkage from a Booking back to the
 * {@code FxRateSnapshot} FIN-04 took at quotation time either, so the
 * caller resolves and passes it through to {@code PaymentsApi.calculateRefund}
 * unchanged (FIN-14, PRD §23.4 Edge Case #9/T15).
 */
public record CalculateCancellationRefundCommand(
    Money sellPrice,
    Instant cancellationDeadline,
    Instant cancelledAt,
    BigDecimal postDeadlinePenaltyPercent,
    FxRateSnapshot originalFxRateSnapshot
) {

    public CalculateCancellationRefundCommand {
        Objects.requireNonNull(sellPrice, "sellPrice must not be null");
        Objects.requireNonNull(cancellationDeadline, "cancellationDeadline must not be null");
        Objects.requireNonNull(cancelledAt, "cancelledAt must not be null");
        Objects.requireNonNull(postDeadlinePenaltyPercent, "postDeadlinePenaltyPercent must not be null");
        Objects.requireNonNull(originalFxRateSnapshot, "originalFxRateSnapshot must not be null");
    }
}
