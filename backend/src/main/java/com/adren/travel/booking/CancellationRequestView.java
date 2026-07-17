package com.adren.travel.booking;

import com.adren.travel.shared.Money;

import java.util.UUID;

/**
 * A cancellation's tracked state (PRD §12.5, FIN-16) — never the JPA
 * entity itself. {@code status} is one of {@code PENDING_APPROVAL},
 * {@code APPROVED}, {@code REFUNDED} as a plain {@code String} rather
 * than an exposed enum type, since {@code CancellationStatus} is an
 * {@code internal} implementation detail (RULES.md §4.1) — the same
 * shape {@code BookingStatus} would use if it were ever surfaced here.
 */
public record CancellationRequestView(
    UUID cancellationRequestId,
    UUID bookingId,
    Money refundAmount,
    Money penaltyAmount,
    String status
) {
}
