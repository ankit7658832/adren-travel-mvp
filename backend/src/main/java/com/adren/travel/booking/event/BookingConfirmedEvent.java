package com.adren.travel.booking.event;

import com.adren.travel.shared.CurrencyCode;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Published when a booking is confirmed (PRD Section 9.1, Flow C, step 5).
 * This is the primary fan-out event of the platform: the Notification module
 * sends the confirmation + voucher (PRD Section 15), the Payments module
 * finalizes the wallet debit (PRD Section 12.3), and the Ads module
 * attributes the booking to a campaign if applicable (PRD Section 20.13).
 * <p>
 * Each listener is independent — a failure in one (e.g., notification
 * delivery) must never roll back the booking itself. See the
 * {@code backend-spring-modulith} skill for the async-listener + event
 * publication registry convention that guarantees this.
 */
public record BookingConfirmedEvent(
    UUID bookingId,
    UUID consultantId,
    BigDecimal totalSellPrice,
    CurrencyCode currency
) {
}
