package com.adren.travel.booking.internal;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * A trackable dispute ticket against a booking (PRD §12.5, FIN-16) —
 * package-private, own table. Deliberately immutable after creation
 * (no setters, no state-transition methods) since {@link DisputeStatus}
 * currently has exactly one reachable value; a resolution workflow would
 * add methods here the same way {@link CancellationRequest} does, not
 * change this class's shape.
 */
@Entity
@Table(name = "dispute_ticket")
class DisputeTicket {

    @Id
    private UUID disputeTicketId;

    private UUID bookingId;
    private UUID consultantId;
    private String reason;

    @Enumerated(EnumType.STRING)
    private DisputeStatus status;

    private Instant createdAt;

    protected DisputeTicket() {
        // JPA
    }

    DisputeTicket(UUID disputeTicketId, UUID bookingId, UUID consultantId, String reason) {
        this.disputeTicketId = disputeTicketId;
        this.bookingId = bookingId;
        this.consultantId = consultantId;
        this.reason = reason;
        this.status = DisputeStatus.OPEN;
        this.createdAt = Instant.now();
    }

    UUID getDisputeTicketId() {
        return disputeTicketId;
    }

    UUID getBookingId() {
        return bookingId;
    }

    UUID getConsultantId() {
        return consultantId;
    }

    String getReason() {
        return reason;
    }

    DisputeStatus getStatus() {
        return status;
    }

    Instant getCreatedAt() {
        return createdAt;
    }
}
