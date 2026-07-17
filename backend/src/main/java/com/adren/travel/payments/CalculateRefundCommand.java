package com.adren.travel.payments;

import com.adren.travel.shared.Money;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Calculates a cancellation's refund/penalty split against the supplier's
 * actual cancellation policy (PRD §12.4/§12.5, FIN-13) — a real policy
 * shape, not a flat percentage. {@code cancellationDeadline} is the line
 * item's own field (PRD §20.2, BOK-03) supplied by the caller: this
 * module has no linkage from a Booking back to its line items yet (no
 * story has built one), so the caller resolves it and passes it in, the
 * same "caller supplies the known value" pattern {@link
 * com.adren.travel.booking.AddHotelLineItemCommand} and BOK-17's FX rates
 * already establish elsewhere in this codebase. {@code
 * postDeadlinePenaltyPercent} is likewise caller-supplied — no per-supplier
 * penalty-percentage table exists anywhere in this codebase (real supplier
 * cancellation-fee schedules are production-tier data this mock phase
 * doesn't model), mirroring FIN-04's "rate supplied by caller" scoping.
 * {@code originalFxRateSnapshot} is the immutable snapshot FIN-04 captured
 * at booking time — FIN-14/PRD §23.4 Edge Case #9 requires the refund's
 * supplier-currency conversion to reuse THIS rate, never a freshly looked
 * up one, even if the market rate has moved since booking. There is
 * deliberately no separate "current rate" parameter anywhere on this
 * command — that is what makes reusing a stale rate structurally
 * impossible rather than a check that has to remember not to re-fetch.
 */
public record CalculateRefundCommand(
    UUID bookingId,
    UUID consultantId,
    Money sellPrice,
    Instant cancellationDeadline,
    Instant cancelledAt,
    BigDecimal postDeadlinePenaltyPercent,
    FxRateSnapshot originalFxRateSnapshot
) {

    public CalculateRefundCommand {
        Objects.requireNonNull(bookingId, "bookingId must not be null");
        Objects.requireNonNull(consultantId, "consultantId must not be null");
        Objects.requireNonNull(sellPrice, "sellPrice must not be null");
        Objects.requireNonNull(cancellationDeadline, "cancellationDeadline must not be null");
        Objects.requireNonNull(cancelledAt, "cancelledAt must not be null");
        Objects.requireNonNull(postDeadlinePenaltyPercent, "postDeadlinePenaltyPercent must not be null");
        Objects.requireNonNull(originalFxRateSnapshot, "originalFxRateSnapshot must not be null");
        if (postDeadlinePenaltyPercent.signum() < 0 || postDeadlinePenaltyPercent.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("postDeadlinePenaltyPercent must be between 0 and 100");
        }
        if (originalFxRateSnapshot.sellCurrency() != sellPrice.currency()) {
            throw new IllegalArgumentException(
                "originalFxRateSnapshot.sellCurrency() must match sellPrice's currency: %s vs %s".formatted(
                    originalFxRateSnapshot.sellCurrency(), sellPrice.currency()));
        }
    }
}
