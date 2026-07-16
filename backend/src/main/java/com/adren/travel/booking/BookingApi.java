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
     * Confirms a booking billed to the Consultant's On-Account balance
     * (PRD §21.4's third payment-method option alongside Stripe/Wallet,
     * §20.8, FIN-12) — same concurrency-safe/tenant-scoped shape as {@link
     * #confirmBooking}, but settles via {@code PaymentsApi.payOnAccount}
     * instead of a wallet hold+debit, and is never gated by FIN-08's
     * credit-limit check (On-Account is a separate settlement path, not
     * wallet balance/credit). Publishes the same {@link
     * com.adren.travel.booking.event.BookingConfirmedEvent}.
     */
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','CONSULTANT','USER')")
    UUID confirmBookingOnAccount(UUID quotationOrPackageId, Money totalSellPrice);

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

    /**
     * Adds a Hotel line item to an itinerary (PRD §20.2, §9.3, BOK-03),
     * pricing it through {@code PaymentsApi.calculateSellRate}'s full
     * net→buffer→markup→commission pipeline (FIN-05). Publishes
     * {@link com.adren.travel.booking.event.HotelLineItemAddedEvent}.
     */
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','CONSULTANT','USER')")
    UUID addHotelLineItem(UUID itineraryId, AddHotelLineItemCommand command);

    /**
     * Adds a Flight line item to an itinerary (PRD §20.3, §10.2.4, BOK-04),
     * priced through the same {@code PaymentsApi.calculateSellRate} pipeline
     * as {@link #addHotelLineItem} under {@code ProductCategory.FLIGHT}.
     * Publishes {@link com.adren.travel.booking.event.FlightLineItemAddedEvent}.
     */
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','CONSULTANT','USER')")
    UUID addFlightLineItem(UUID itineraryId, AddFlightLineItemCommand command);

    /**
     * Adds a Transfer line item to an itinerary (PRD §20.4, §10.2.5, BOK-05),
     * priced through the same {@code PaymentsApi.calculateSellRate} pipeline
     * under {@code ProductCategory.TRANSFER}. Publishes {@link
     * com.adren.travel.booking.event.TransferLineItemAddedEvent}.
     */
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','CONSULTANT','USER')")
    UUID addTransferLineItem(UUID itineraryId, AddTransferLineItemCommand command);

    /**
     * Adds a Cruise line item to an itinerary (PRD §20.5, §10.2.6, BOK-06),
     * priced through the same {@code PaymentsApi.calculateSellRate} pipeline
     * under {@code ProductCategory.CRUISE}. Publishes {@link
     * com.adren.travel.booking.event.CruiseLineItemAddedEvent}.
     */
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','CONSULTANT','USER')")
    UUID addCruiseLineItem(UUID itineraryId, AddCruiseLineItemCommand command);

    /**
     * Adds an Activity line item to an itinerary (PRD §20.6, §10.2.7, BOK-07),
     * priced through the same {@code PaymentsApi.calculateSellRate} pipeline
     * under {@code ProductCategory.ACTIVITY}. Publishes {@link
     * com.adren.travel.booking.event.ActivityLineItemAddedEvent}.
     */
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','CONSULTANT','USER')")
    UUID addActivityLineItem(UUID itineraryId, AddActivityLineItemCommand command);

    /**
     * Changes an Activity line item's headcount (PRD §20.6, §10.2.7, BOK-07)
     * — blocked once the owning itinerary has left DRAFT (the same
     * immutability boundary {@link #addActivityLineItem} and every other
     * {@code add*LineItem} method already enforces), matching most
     * suppliers' fixed-at-booking headcount constraint.
     */
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','CONSULTANT','USER')")
    void updateActivityHeadcount(UUID itineraryId, UUID lineItemId, int newHeadcount);

    /**
     * Consolidates an itinerary's (possibly mixed-sell-currency) line items
     * into one total in the Consultant's sell currency (PRD §23.1 Edge Case
     * #2, BOK-17) — the checkout screen calls this to compute the value it
     * then passes as {@link #confirmBooking}'s {@code totalSellPrice},
     * rather than {@code confirmBooking} itself performing conversion
     * inline (its signature is an existing, already-exercised public
     * contract; this stays a separate, composable step upstream of it).
     */
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','CONSULTANT','USER')")
    Money consolidateCheckoutTotal(ConsolidateCheckoutTotalCommand command);

    /**
     * Records a traveler-count change on an existing Quotation (PRD §23.1
     * Edge Case #3, BOK-18) — blocked once the underlying itinerary has
     * reached BOOKED (the "before booking" boundary the story's AC names).
     * Resets the Quotation's FX/price validity window to a fresh one from
     * now, so a stale window can never silently carry over past this
     * change. Publishes {@link com.adren.travel.booking.event.QuotationRecalculatedEvent}.
     */
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','CONSULTANT','USER')")
    void recalculateQuotation(UUID quotationId, int newTravelerCount);

    /**
     * Marks a Package's ATOL disclosure step complete (PRD §17.2, §22.3 T5,
     * BOK-11) — the precondition {@link #publishPackage} checks for a UK
     * Consultant's dynamic flight+hotel package. Same "Create package"
     * role/capability-grant shape as {@link #publishPackage} itself.
     */
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','CONSULTANT') or "
        + "(hasRole('USER') and @capabilityGrantService.isGranted(principal.userId, "
        + "T(com.adren.travel.security.CapabilityGrantService.Capability).CREATE_PACKAGE))")
    void completeAtolDisclosure(UUID packageId);

    /**
     * Confirms a booking once a Stripe webhook (not a direct user request)
     * reports payment succeeded (PRD §12.4, FIN-11) — invoked by this
     * module's own listener on {@code payments.event.StripePaymentSucceededEvent},
     * not by an authenticated Adren principal, so unlike {@link
     * #confirmBooking} this carries no {@code @PreAuthorize} and no tenant-
     * active gate: there is no {@code CurrentPrincipal} on an async event
     * listener's thread. Publishes the same {@link
     * com.adren.travel.booking.event.BookingConfirmedEvent}.
     */
    UUID confirmBookingFromPaymentWebhook(UUID quotationOrPackageId, UUID consultantId, Money totalSellPrice);

    /**
     * Converts a saved Quotation into a reusable Package (PRD §9.1 Flow B,
     * §20.7, BOK-10), not yet published. PRD §6's role matrix: "Create
     * package" is {@code Yes/Yes/No (unless granted)} — a {@code USER} is
     * allowed only if the calling Consultant has granted them {@link
     * com.adren.travel.security.CapabilityGrantService.Capability#CREATE_PACKAGE}
     * (RULES.md §5.1 — data-driven, not a hardcoded role {@code switch}).
     * Publishes {@link com.adren.travel.booking.event.PackageCreatedEvent}.
     */
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','CONSULTANT') or "
        + "(hasRole('USER') and @capabilityGrantService.isGranted(principal.userId, "
        + "T(com.adren.travel.security.CapabilityGrantService.Capability).CREATE_PACKAGE))")
    UUID convertQuotationToPackage(UUID quotationId, ConvertQuotationToPackageCommand command);

    /**
     * Publishes a Package, making it visible to the Consultant's Users
     * (PRD §9.1 Flow B step 3, §22.3, BOK-12) — same "Create package"
     * role/capability-grant shape as {@link #convertQuotationToPackage},
     * since publishing is part of the same authority. {@code promoteViaAds}
     * records whether the Consultant opted into Meta campaign promotion
     * (PRD §20.7); the actual hand-off into the Ads Campaign Builder
     * (ADS-03) is a frontend navigation concern. The UK ATOL disclosure
     * gate (BOK-11) is deferred — this vertical slice has no Flight line
     * item type yet, so a dynamic flight+hotel combo can never occur.
     * Publishes {@link com.adren.travel.booking.event.PackagePublishedEvent}.
     */
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','CONSULTANT') or "
        + "(hasRole('USER') and @capabilityGrantService.isGranted(principal.userId, "
        + "T(com.adren.travel.security.CapabilityGrantService.Capability).CREATE_PACKAGE))")
    UUID publishPackage(UUID packageId, boolean promoteViaAds);

    /**
     * The Consultant's PUBLISHED packages, visible to Users for booking
     * (PRD §9.1 Flow B step 3, §22.3, BOK-12) — {@code Yes/Yes/Yes} across
     * Super Admin/Consultant/User, unlike creating/publishing a package,
     * since viewing what's already for sale is the same "make a booking"
     * access every role has. Paginated per RULES.md §3.4.
     */
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','CONSULTANT','USER')")
    Page<PackageView> findPublishedPackagesByConsultant(UUID consultantId, Pageable pageable);
}
