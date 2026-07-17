package com.adren.travel.booking.event;

import com.adren.travel.shared.Money;

import java.util.UUID;

/**
 * Published once a cancellation's refund has actually been processed
 * (PRD §12.5, FIN-16) — not on submission or approval, only once money has
 * moved. Not yet consumed by {@code notification} — wiring a notification
 * dispatch on refund completion is HRD-05's "full cancellation workflow"
 * scope (see its own Javadoc/story), not this one's.
 */
public record BookingCancelledEvent(UUID bookingId, UUID consultantId, Money refundAmount, Money penaltyAmount) {
}
