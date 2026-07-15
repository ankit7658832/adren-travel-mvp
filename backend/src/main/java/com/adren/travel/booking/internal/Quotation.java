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
        this.createdAt = Instant.now();
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
}
