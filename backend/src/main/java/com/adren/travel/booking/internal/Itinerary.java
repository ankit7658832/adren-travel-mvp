package com.adren.travel.booking.internal;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Itinerary entity per PRD Section 20.1 (Complete Data Dictionary).
 * Package-private visibility is intentional where possible — this class is
 * public only because JPA requires it, but it must never be referenced
 * outside {@code com.adren.travel.booking.internal}. Other modules interact
 * with itineraries only through {@link com.adren.travel.booking.BookingApi}.
 */
@Entity
@Table(name = "itinerary")
class Itinerary {

    @Id
    private UUID itineraryId;

    private UUID consultantId;
    private UUID createdByUserId;
    private Instant dateRangeStart;
    private Instant dateRangeEnd;

    @Enumerated(EnumType.STRING)
    private ItineraryStatus status;

    private boolean aiGenerated;
    private UUID aiAuditLogId;

    private Instant createdAt;
    private Instant updatedAt;

    protected Itinerary() {
        // JPA
    }

    Itinerary(UUID itineraryId, UUID consultantId, UUID createdByUserId) {
        this.itineraryId = itineraryId;
        this.consultantId = consultantId;
        this.createdByUserId = createdByUserId;
        this.status = ItineraryStatus.DRAFT;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    void markAsQuotation() {
        if (this.status != ItineraryStatus.DRAFT) {
            throw new IllegalStateException(
                "Only a DRAFT itinerary can become a QUOTATION, was: " + this.status);
        }
        this.status = ItineraryStatus.QUOTATION;
        this.updatedAt = Instant.now();
    }

    UUID getItineraryId() {
        return itineraryId;
    }

    UUID getConsultantId() {
        return consultantId;
    }

    ItineraryStatus getStatus() {
        return status;
    }
}
