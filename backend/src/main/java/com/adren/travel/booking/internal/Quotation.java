package com.adren.travel.booking.internal;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * A Quotation created when an Itinerary is saved (PRD §20.9, BOK-09) —
 * package-private, own table. {@code validUntil} is this quotation's
 * rate/FX validity window (PRD §12.2/§22.4 T7 — the {@code FxRateSnapshot}
 * a booking priced against must have an explicit expiry, not stand forever).
 * <p>
 * {@code travelerCount} (BOK-18, PRD §23.1 Edge Case #3) defaults to 1 —
 * {@code saveAsQuotation} never captured a traveler count at quotation
 * time, a documented assumption the same shape as {@code validUntil}'s own
 * 7-day default in {@code BookingServiceImpl}.
 */
@Entity
@Table(name = "quotation")
class Quotation {

    @Id
    private UUID quotationId;

    private UUID itineraryId;
    private Instant validUntil;
    private boolean sharedWithTraveler;
    private UUID convertedToBookingId;
    private int travelerCount;
    private Instant createdAt;

    protected Quotation() {
        // JPA
    }

    Quotation(UUID quotationId, UUID itineraryId, Instant validUntil) {
        this.quotationId = quotationId;
        this.itineraryId = itineraryId;
        this.validUntil = validUntil;
        this.sharedWithTraveler = false;
        this.convertedToBookingId = null;
        this.travelerCount = 1;
        this.createdAt = Instant.now();
    }

    /**
     * Traveler count changed after this Quotation was generated (PRD §23.1
     * Edge Case #3) — records the new count and resets {@code validUntil}
     * to a fresh window from now, so nothing downstream can treat the
     * pre-change validity window as still current. Callers are responsible
     * for the "not yet booked" precondition (BOK-16's real {@code
     * Itinerary.status == BOOKED} signal, not this class's own unused
     * {@code convertedToBookingId} field, which nothing in this codebase
     * currently sets — see {@code BookingServiceImpl.recalculateQuotation}).
     * Genuine per-traveler cost re-pricing isn't modeled anywhere in FIN-05's
     * pricing pipeline today (line items price independent of headcount,
     * except {@code ActivityLineItem}'s own supplier-specific field) — this
     * method's job is the mechanically real part: forcing fresh pricing
     * validity rather than fabricating a speculative per-head formula.
     */
    void recalculate(int newTravelerCount, Instant newValidUntil) {
        if (newTravelerCount <= 0) {
            throw new IllegalArgumentException("travelerCount must be a positive value");
        }
        this.travelerCount = newTravelerCount;
        this.validUntil = newValidUntil;
    }

    UUID getQuotationId() {
        return quotationId;
    }

    UUID getItineraryId() {
        return itineraryId;
    }

    Instant getValidUntil() {
        return validUntil;
    }

    boolean isSharedWithTraveler() {
        return sharedWithTraveler;
    }

    UUID getConvertedToBookingId() {
        return convertedToBookingId;
    }

    int getTravelerCount() {
        return travelerCount;
    }
}
