package com.adren.travel.booking;

import java.util.Objects;

/**
 * Inputs to {@link BookingApi#flagDispute} (PRD §12.5, FIN-16).
 * {@code consultantId} is deliberately not a field here — unlike
 * cancellation, a dispute is always resolved against the booking's own
 * consultant (there's no caller-supplied identity to trust), the same
 * "resolve tenant scope from the looked-up record" shape {@code
 * calculateCancellationRefund} already uses.
 */
public record FlagDisputeCommand(String reason) {

    public FlagDisputeCommand {
        Objects.requireNonNull(reason, "reason must not be null");
        if (reason.isBlank()) {
            throw new IllegalArgumentException("reason must not be blank");
        }
    }
}
