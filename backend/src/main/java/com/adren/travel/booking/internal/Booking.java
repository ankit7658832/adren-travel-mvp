package com.adren.travel.booking.internal;

import com.adren.travel.shared.CurrencyCode;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * A confirmed Booking (PRD §20.8, BOK-19) — package-private, own table.
 * Scoped to what BOK-19 needs ({@code pnrSearchableRef}) plus the core
 * identity/amount fields {@code confirmBooking}/{@code
 * confirmBookingFromPaymentWebhook} already have on hand; {@code
 * traveler_ids[]}/{@code supplier_booking_refs[]} from the full PRD §20.8
 * field set are a documented omission — no story currently populates them.
 * {@code itineraryId} is nullable because the Stripe-webhook confirmation
 * path doesn't resolve it (see {@code BookingServiceImpl}'s BOK-16 scoping
 * note on why extending that path is a separate change).
 */
@Entity
@Table(name = "booking")
class Booking {

    @Id
    private UUID bookingId;

    private UUID itineraryId;
    private UUID consultantId;

    @Enumerated(EnumType.STRING)
    private BookingStatus status;

    @Column(name = "total_sell_price")
    private BigDecimal totalSellPriceAmount;

    @Enumerated(EnumType.STRING)
    private CurrencyCode totalSellCurrency;

    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    private String pnrSearchableRef;
    private Instant createdAt;

    protected Booking() {
        // JPA
    }

    Booking(UUID bookingId, UUID itineraryId, UUID consultantId, BigDecimal totalSellPriceAmount,
            CurrencyCode totalSellCurrency, PaymentMethod paymentMethod, String pnrSearchableRef) {
        this.bookingId = bookingId;
        this.itineraryId = itineraryId;
        this.consultantId = consultantId;
        this.status = BookingStatus.CONFIRMED;
        this.totalSellPriceAmount = totalSellPriceAmount;
        this.totalSellCurrency = totalSellCurrency;
        this.paymentMethod = paymentMethod;
        this.pnrSearchableRef = pnrSearchableRef;
        this.createdAt = Instant.now();
    }

    /**
     * A cancellation's refund has been processed (FIN-16, PRD §12.5) —
     * only reachable from {@code CONFIRMED}, matching backend-best-practices
     * §1's "throw on an invalid transition, never silently no-op" rule.
     */
    void markCancelled() {
        if (status != BookingStatus.CONFIRMED) {
            throw new IllegalStateException(
                "Cannot cancel booking %s: status is %s, expected CONFIRMED".formatted(bookingId, status));
        }
        this.status = BookingStatus.CANCELLED;
    }

    /**
     * A dispute has been flagged on this booking (FIN-16, PRD §12.5) —
     * only reachable from {@code CONFIRMED}; a booking that's already been
     * cancelled or is already disputed cannot be flagged again.
     */
    void markDisputed() {
        if (status != BookingStatus.CONFIRMED) {
            throw new IllegalStateException(
                "Cannot flag booking %s as disputed: status is %s, expected CONFIRMED".formatted(bookingId, status));
        }
        this.status = BookingStatus.DISPUTED;
    }

    UUID getBookingId() {
        return bookingId;
    }

    UUID getItineraryId() {
        return itineraryId;
    }

    UUID getConsultantId() {
        return consultantId;
    }

    BookingStatus getStatus() {
        return status;
    }

    String getPnrSearchableRef() {
        return pnrSearchableRef;
    }

    PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    BigDecimal getTotalSellPriceAmount() {
        return totalSellPriceAmount;
    }

    CurrencyCode getTotalSellCurrency() {
        return totalSellCurrency;
    }

    Instant getCreatedAt() {
        return createdAt;
    }
}
