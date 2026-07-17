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
    private boolean aiApproved;

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

    /**
     * Records that an AI suggestion was generated for this itinerary
     * (AI-02) — {@code aiApproved} deliberately stays false here even on a
     * repeat call, since a fresh suggestion supersedes whatever was
     * approved (or not) before it. Idempotent-shaped (always safe to call
     * again), matching the "AI may be asked to regenerate" reality of the
     * "Complete with AI" flow (AI-10) rather than throwing on a second call.
     */
    void markAiGenerated(UUID auditLogId) {
        this.aiGenerated = true;
        this.aiAuditLogId = auditLogId;
        this.aiApproved = false;
        this.updatedAt = Instant.now();
    }

    /**
     * A Consultant/permitted User has explicitly approved the current AI
     * suggestion (AI-06, PRD §11.2 principle 3) — the only thing {@link
     * #markAsQuotation()}'s AI gate checks. Throws rather than silently
     * no-op'ing if there is no AI suggestion to approve (backend-best-
     * practices §1) — approving nothing is a caller error, not a valid
     * no-op transition.
     */
    void markAiApproved() {
        if (!this.aiGenerated) {
            throw new IllegalStateException(
                "Cannot approve an AI suggestion for itinerary " + itineraryId + ": none was generated");
        }
        this.aiApproved = true;
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

    boolean isAiGenerated() {
        return aiGenerated;
    }

    UUID getAiAuditLogId() {
        return aiAuditLogId;
    }

    boolean isAiApproved() {
        return aiApproved;
    }
}
