package com.adren.travel.booking.event;

import java.util.UUID;

/** Published when a dispute ticket is created (PRD §12.5, FIN-16) — consumed by {@code notification} to alert the Consultant. */
public record DisputeTicketCreatedEvent(UUID disputeTicketId, UUID bookingId, UUID consultantId, String reason) {
}
