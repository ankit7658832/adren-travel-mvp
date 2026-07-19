package com.adren.travel.booking.event;

import com.adren.travel.shared.Money;

import java.util.UUID;

/**
 * Published once a cancellation's refund has actually been processed
 * (PRD §12.5, FIN-16) — not on submission or approval, only once money has
 * moved. Consumed by {@code notification.internal.BookingCancelledNotificationListener}
 * (HRD-05) to alert the Consultant.
 */
public record BookingCancelledEvent(UUID bookingId, UUID consultantId, Money refundAmount, Money penaltyAmount) {
}
