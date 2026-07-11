package com.adren.travel.booking.internal;

import com.adren.travel.booking.BookingApi;
import com.adren.travel.booking.event.BookingConfirmedEvent;
import com.adren.travel.booking.event.ItineraryQuotationSavedEvent;
import com.adren.travel.shared.Money;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

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

    BookingServiceImpl(ItineraryRepository itineraryRepository, ApplicationEventPublisher events) {
        this.itineraryRepository = itineraryRepository;
        this.events = events;
    }

    @Override
    public UUID saveAsQuotation(UUID itineraryId) {
        Itinerary itinerary = itineraryRepository.findById(itineraryId)
            .orElseThrow(() -> new IllegalArgumentException("No itinerary: " + itineraryId));

        itinerary.markAsQuotation();
        itineraryRepository.save(itinerary);

        UUID quotationId = UUID.randomUUID(); // simplified — a real Quotation entity would be created here
        events.publishEvent(new ItineraryQuotationSavedEvent(itineraryId, quotationId, itinerary.getConsultantId()));
        return quotationId;
    }

    @Override
    public UUID confirmBooking(UUID quotationOrPackageId, Money totalSellPrice) {
        UUID bookingId = UUID.randomUUID(); // simplified — a real Booking entity would be created/persisted here
        UUID consultantId = UUID.randomUUID(); // resolved from the quotation/package in a full implementation

        events.publishEvent(new BookingConfirmedEvent(
            bookingId, consultantId, totalSellPrice.amount(), totalSellPrice.currency()));
        return bookingId;
    }

    @Override
    public List<UUID> findBookingsByConsultant(UUID consultantId) {
        return itineraryRepository.findByConsultantId(consultantId).stream()
            .map(Itinerary::getItineraryId)
            .toList();
    }
}
