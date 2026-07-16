package com.adren.travel.booking.internal;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

/**
 * Itinerary entity per PRD Section 20.1 (Complete Data Dictionary).
 * Package-private visibility is intentional where possible — this class is
 * public only because JPA requires it, but it must never be referenced
 * outside {@code com.adren.travel.booking.internal}. Other modules interact
 * with itineraries only through {@link com.adren.travel.booking.BookingApi}.
 * <p>
 * {@code version} (BOK-16, PRD §23.1 Edge Case #1) is JPA optimistic
 * locking, not just documentation — {@link #markAsBooked()} is called from
 * {@code BookingServiceImpl}'s confirmation paths and saved via {@code
 * saveAndFlush}, so two concurrent confirmations of the SAME itinerary race
 * on this column: the second writer's {@code UPDATE ... WHERE version = ?}
 * matches zero rows and JPA throws {@code OptimisticLockException}, which
 * the service layer maps to a "no longer available" domain exception rather
 * than a duplicate confirmation succeeding silently.
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

    @Version
    private long version;

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

    // BOK-16 — both confirmBooking (quotation-derived) and
    // confirmBookingFromPaymentWebhook/the package path leave the source
    // itinerary at QUOTATION (convertQuotationToPackage/publishPackage
    // never touch itinerary status), so QUOTATION is the correct
    // precondition for either confirmation path.
    void markAsBooked() {
        if (this.status != ItineraryStatus.QUOTATION) {
            throw new IllegalStateException(
                "Only a QUOTATION itinerary can become BOOKED, was: " + this.status);
        }
        this.status = ItineraryStatus.BOOKED;
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
