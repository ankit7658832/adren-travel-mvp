package com.adren.travel.payments.event;

import com.adren.travel.shared.Money;

import java.util.UUID;

/** Published once a calculated refund (FIN-13/14) is actually credited back to a Consultant's wallet (PRD §12.5, FIN-16) — a money movement, unlike {@link RefundCalculatedEvent}. */
public record RefundProcessedEvent(UUID bookingId, UUID consultantId, Money amount) {
}
