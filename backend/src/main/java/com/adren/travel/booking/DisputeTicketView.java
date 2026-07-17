package com.adren.travel.booking;

import java.time.Instant;
import java.util.UUID;

/** A tracked dispute ticket (PRD §12.5, FIN-16) — never the JPA entity itself. */
public record DisputeTicketView(UUID disputeTicketId, UUID bookingId, String reason, String status, Instant createdAt) {
}
