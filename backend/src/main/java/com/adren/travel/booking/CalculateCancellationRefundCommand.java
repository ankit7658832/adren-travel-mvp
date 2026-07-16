package com.adren.travel.booking;

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
 * scoping note this mirrors).
 */
public record CalculateCancellationRefundCommand(
    Money sellPrice,
    Instant cancellationDeadline,
    Instant cancelledAt,
    BigDecimal postDeadlinePenaltyPercent
) {

    public CalculateCancellationRefundCommand {
        Objects.requireNonNull(sellPrice, "sellPrice must not be null");
        Objects.requireNonNull(cancellationDeadline, "cancellationDeadline must not be null");
        Objects.requireNonNull(cancelledAt, "cancelledAt must not be null");
        Objects.requireNonNull(postDeadlinePenaltyPercent, "postDeadlinePenaltyPercent must not be null");
    }
}
