package com.adren.travel.booking;

import com.adren.travel.shared.Money;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Public API of the Booking module. Other modules (Payments, Notification,
 * Ads) must depend on this interface, never on classes under
 * {@code com.adren.travel.booking.internal}.
 */
public interface BookingApi {

    /**
     * Saves an in-progress itinerary as a Quotation (PRD Section 9.1, Flow A,
     * step 8-9). Publishes {@link com.adren.travel.booking.event.ItineraryQuotationSavedEvent}.
     */
    UUID saveAsQuotation(UUID itineraryId);

    /**
     * Confirms a booking from a Quotation or Package after payment succeeds.
     * Publishes {@link com.adren.travel.booking.event.BookingConfirmedEvent},
     * which the Notification and Payments modules react to independently
     * (PRD Section 15 — event-driven notification fan-out).
     */
    UUID confirmBooking(UUID quotationOrPackageId, Money totalSellPrice);

    /**
     * Paginated per RULES.md §3.4 — never a bare {@code List<UUID>} at a
     * public Api boundary a controller might wire up unbounded, given a
     * Consultant can accumulate thousands of bookings over time.
     */
    Page<UUID> findBookingsByConsultant(UUID consultantId, Pageable pageable);
}
