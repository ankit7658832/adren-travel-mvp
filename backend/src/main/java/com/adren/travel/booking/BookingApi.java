package com.adren.travel.booking;

import com.adren.travel.shared.Money;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Public API of the Booking module. Other modules (Payments, Notification,
 * Ads) must depend on this interface, never on classes under
 * {@code com.adren.travel.booking.internal}.
 * <p>
 * Every method carries an explicit {@code @PreAuthorize} matching PRD §6's
 * role matrix (RULES.md §5.1) — enforced here, on the Api interface, so
 * every caller (a future scheduled job, another module, a controller)
 * inherits the same check, rather than trusting each controller author to
 * remember it. Per §6, "Search &amp; build itinerary" and "Make booking" are
 * both Yes/Yes/Yes across Super Admin/Consultant/User — that's still
 * declared explicitly below rather than left as "no annotation means
 * public," because an unannotated method is indistinguishable from one
 * nobody has reviewed yet.
 */
public interface BookingApi {

    /**
     * Saves an in-progress itinerary as a Quotation (PRD Section 9.1, Flow A,
     * step 8-9). Publishes {@link com.adren.travel.booking.event.ItineraryQuotationSavedEvent}.
     */
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','CONSULTANT','USER')")
    UUID saveAsQuotation(UUID itineraryId);

    /**
     * Confirms a booking from a Quotation or Package after payment succeeds.
     * Publishes {@link com.adren.travel.booking.event.BookingConfirmedEvent},
     * which the Notification and Payments modules react to independently
     * (PRD Section 15 — event-driven notification fan-out).
     */
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','CONSULTANT','USER')")
    UUID confirmBooking(UUID quotationOrPackageId, Money totalSellPrice);

    /**
     * Paginated per RULES.md §3.4 — never a bare {@code List<UUID>} at a
     * public Api boundary a controller might wire up unbounded, given a
     * Consultant can accumulate thousands of bookings over time.
     */
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','CONSULTANT','USER')")
    Page<UUID> findBookingsByConsultant(UUID consultantId, Pageable pageable);

    /**
     * The Itinerary Builder's alternate-selection side panel (PRD §21.2,
     * FND-16) — every alternate available for one location/category, for
     * the Consultant to filter/sort (price, rating, supplier) client-side
     * and swap the location's current line item for. {@code category} is
     * accepted for PRD §21.2's up-to-five-product-type URL shape but only
     * {@code "hotel"} has real inventory today (PRD §10.5's supplier
     * content sync for other categories is production-tier, not yet
     * built) — any other category returns an empty list rather than
     * erroring, matching {@code GeocodeAndSearchService}'s
     * never-throw-on-no-inventory convention.
     */
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','CONSULTANT','USER')")
    List<AlternateOption> findAlternates(
        UUID itineraryId, String locationCode, String category, LocalDate checkIn, LocalDate checkOut);

    /**
     * Captures a Traveler Profile (PRD §20.10, BOK-14), scoped to the
     * CALLING principal's own consultantId — never a client-supplied one,
     * see {@link CreateTravelerProfileCommand}'s Javadoc. Publishes
     * {@link com.adren.travel.booking.event.TravelerProfileCreatedEvent}.
     */
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','CONSULTANT','USER')")
    UUID createTravelerProfile(CreateTravelerProfileCommand command);
}
