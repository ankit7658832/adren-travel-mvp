package com.adren.travel.booking.internal;

import com.adren.travel.shared.CurrencyCode;
import com.adren.travel.supplier.SupplierId;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

/**
 * An Activity line item on an Itinerary (PRD §20.6, §10.2.7, BOK-07) —
 * package-private, own table. Same pricing-field shape as {@link
 * HotelLineItem}; see its Javadoc for why.
 */
@Entity
@Table(name = "activity_line_item")
class ActivityLineItem {

    @Id
    private UUID lineItemId;

    private UUID itineraryId;

    @Enumerated(EnumType.STRING)
    private SupplierId supplierId;

    private String supplierRateId;
    private int durationMinutes;
    private LocalTime timeSlot;
    private int headcount;

    private BigDecimal netRate;

    @Enumerated(EnumType.STRING)
    private CurrencyCode netRateCurrency;

    private BigDecimal markupApplied;
    private BigDecimal currencyBufferApplied;
    private BigDecimal sellRate;

    @Enumerated(EnumType.STRING)
    private CurrencyCode sellCurrency;

    private BigDecimal fxRateSnapshot;

    private Instant createdAt;

    protected ActivityLineItem() {
        // JPA
    }

    ActivityLineItem(UUID lineItemId, UUID itineraryId, SupplierId supplierId, String supplierRateId,
                      int durationMinutes, LocalTime timeSlot, int headcount, BigDecimal netRate,
                      CurrencyCode netRateCurrency, BigDecimal markupApplied, BigDecimal currencyBufferApplied,
                      BigDecimal sellRate, CurrencyCode sellCurrency, BigDecimal fxRateSnapshot) {
        this.lineItemId = lineItemId;
        this.itineraryId = itineraryId;
        this.supplierId = supplierId;
        this.supplierRateId = supplierRateId;
        this.durationMinutes = durationMinutes;
        this.timeSlot = timeSlot;
        this.headcount = headcount;
        this.netRate = netRate;
        this.netRateCurrency = netRateCurrency;
        this.markupApplied = markupApplied;
        this.currencyBufferApplied = currencyBufferApplied;
        this.sellRate = sellRate;
        this.sellCurrency = sellCurrency;
        this.fxRateSnapshot = fxRateSnapshot;
        this.createdAt = Instant.now();
    }

    UUID getLineItemId() {
        return lineItemId;
    }

    UUID getItineraryId() {
        return itineraryId;
    }

    int getDurationMinutes() {
        return durationMinutes;
    }

    LocalTime getTimeSlot() {
        return timeSlot;
    }

    int getHeadcount() {
        return headcount;
    }

    /**
     * Most HBActivities supplier contracts fix headcount at booking time and
     * don't allow post-confirmation amendment (PRD §10.2.7) — enforced here
     * against the same DRAFT-only immutability boundary
     * {@code BookingServiceImpl.requireOwnedDraftItinerary} already applies
     * to adding a line item in the first place ("once saved as a Quotation,
     * an itinerary is read-only except via explicit edit," BOK-08). A
     * stricter "locked only once payment/booking is actually confirmed, not
     * merely quoted" reading would need booking-level state tracking this
     * codebase doesn't have yet (see BOK-13's own "simplified — a real
     * Booking entity would be created/persisted here" note) — this method's
     * caller is responsible for that DRAFT check; this method itself is a
     * plain field mutation with a positive-headcount invariant.
     */
    void updateHeadcount(int newHeadcount) {
        if (newHeadcount <= 0) {
            throw new IllegalArgumentException("headcount must be a positive value");
        }
        this.headcount = newHeadcount;
    }

    BigDecimal getSellRate() {
        return sellRate;
    }

    CurrencyCode getSellCurrency() {
        return sellCurrency;
    }

    BigDecimal getMarkupApplied() {
        return markupApplied;
    }

    BigDecimal getCurrencyBufferApplied() {
        return currencyBufferApplied;
    }

    BigDecimal getFxRateSnapshot() {
        return fxRateSnapshot;
    }
}
