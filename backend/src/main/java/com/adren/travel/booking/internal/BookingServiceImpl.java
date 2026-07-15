package com.adren.travel.booking.internal;

import com.adren.travel.booking.AlternateOption;
import com.adren.travel.booking.BookingApi;
import com.adren.travel.booking.event.BookingConfirmedEvent;
import com.adren.travel.booking.event.ItineraryQuotationSavedEvent;
import com.adren.travel.security.AdrenPrincipal;
import com.adren.travel.security.CurrentPrincipal;
import com.adren.travel.shared.Money;
import com.adren.travel.supplier.SupplierSearchApi;
import com.adren.travel.whitelabel.WhitelabelApi;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Internal implementation of {@link BookingApi}. Not visible outside this
 * module — Spring wires it in wherever {@code BookingApi} is injected.
 * <p>
 * Event publication uses {@link ApplicationEventPublisher} backed by Spring
 * Modulith's JPA event publication registry (see build.gradle.kts
 * spring-modulith-starter-jpa) so events are persisted transactionally with
 * the state change and redelivered on listener failure — this is what makes
 * the architecture genuinely event-driven rather than just
 * fire-and-forget in-process pub/sub.
 */
@Service
class BookingServiceImpl implements BookingApi {

    private final ItineraryRepository itineraryRepository;
    private final ApplicationEventPublisher events;
    private final WhitelabelApi whitelabelApi;
    private final SupplierSearchApi supplierSearchApi;

    BookingServiceImpl(ItineraryRepository itineraryRepository, ApplicationEventPublisher events,
                        WhitelabelApi whitelabelApi, SupplierSearchApi supplierSearchApi) {
        this.itineraryRepository = itineraryRepository;
        this.events = events;
        this.whitelabelApi = whitelabelApi;
        this.supplierSearchApi = supplierSearchApi;
    }

    @Override
    @Transactional
    public UUID saveAsQuotation(UUID itineraryId) {
        Itinerary itinerary = itineraryRepository.findById(itineraryId)
            .orElseThrow(() -> new IllegalArgumentException("No itinerary: " + itineraryId));

        // RULES.md §5.2 / FND-03: verify the AUTHENTICATED principal owns
        // this itinerary (or is SUPER_ADMIN) before acting on it — an
        // itinerary_id alone is not an access-control mechanism, it's just
        // a key that's hard to guess.
        CurrentPrincipal.resolveTenantScope(itinerary.getConsultantId());
        requireActiveUnlessSuperAdmin(itinerary.getConsultantId());

        itinerary.markAsQuotation();
        itineraryRepository.save(itinerary);

        UUID quotationId = UUID.randomUUID(); // simplified — a real Quotation entity would be created here
        events.publishEvent(new ItineraryQuotationSavedEvent(itineraryId, quotationId, itinerary.getConsultantId()));
        return quotationId;
    }

    @Override
    @Transactional
    public UUID confirmBooking(UUID quotationOrPackageId, Money totalSellPrice) {
        AdrenPrincipal principal = CurrentPrincipal.get();
        requireActiveUnlessSuperAdmin(principal.consultantId());

        UUID bookingId = UUID.randomUUID(); // simplified — a real Booking entity would be created/persisted here
        UUID consultantId = UUID.randomUUID(); // resolved from the quotation/package in a full implementation

        events.publishEvent(new BookingConfirmedEvent(
            bookingId, consultantId, totalSellPrice.amount(), totalSellPrice.currency()));
        return bookingId;
    }

    // FND-05 — a SUSPENDED Consultant's Users can no longer search/book;
    // SUPER_ADMIN has no consultantId and is exempt from this gate.
    private void requireActiveUnlessSuperAdmin(UUID consultantId) {
        if (!CurrentPrincipal.get().isSuperAdmin()) {
            whitelabelApi.requireConsultantActive(consultantId);
        }
    }

    @Override
    public List<AlternateOption> findAlternates(
        UUID itineraryId, String locationCode, String category, LocalDate checkIn, LocalDate checkOut) {
        requireActiveUnlessSuperAdmin(CurrentPrincipal.get().consultantId());

        if (category != null && !category.isBlank() && !category.equalsIgnoreCase("hotel")) {
            return List.of();
        }
        return supplierSearchApi.searchHotels(locationCode, checkIn, checkOut).stream()
            .map(result -> new AlternateOption(result.supplierId().name(), result.supplierRateId(),
                result.propertyName(), result.roomType(), result.netRate().amount(), result.netRate().currency(),
                result.rating()))
            .toList();
    }

    @Override
    public Page<UUID> findBookingsByConsultant(UUID consultantId, Pageable pageable) {
        // RULES.md §5.2: never trust a client-supplied consultantId for a
        // CONSULTANT/USER caller — resolveTenantScope rejects any mismatch
        // and only lets SUPER_ADMIN's explicit "view all" path through
        // with the requested id unchanged.
        UUID scopedConsultantId = CurrentPrincipal.resolveTenantScope(consultantId);
        return itineraryRepository.findByConsultantId(scopedConsultantId, pageable)
            .map(Itinerary::getItineraryId);
    }
}
