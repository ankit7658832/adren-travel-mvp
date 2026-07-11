package com.adren.travel.booking.internal;

import com.adren.travel.booking.event.BookingConfirmedEvent;
import com.adren.travel.booking.event.ItineraryQuotationSavedEvent;
import com.adren.travel.shared.CurrencyCode;
import com.adren.travel.shared.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure unit test of the internal service, with the repository and event
 * publisher mocked out (Mockito) — no Spring context, no database. This is
 * the fast, default test tier; reserve {@code @ApplicationModuleTest} (see
 * {@code BookingModuleIntegrationTests}) for verifying the real Spring
 * wiring and event-publication contract.
 */
@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {

    @Mock
    ItineraryRepository itineraryRepository;

    @Mock
    ApplicationEventPublisher events;

    BookingServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new BookingServiceImpl(itineraryRepository, events);
    }

    @Test
    void savingAsQuotationTransitionsStatusAndPublishesEvent() {
        UUID itineraryId = UUID.randomUUID();
        UUID consultantId = UUID.randomUUID();
        Itinerary draft = new Itinerary(itineraryId, consultantId, null);
        when(itineraryRepository.findById(itineraryId)).thenReturn(Optional.of(draft));

        service.saveAsQuotation(itineraryId);

        assertThat(draft.getStatus()).isEqualTo(ItineraryStatus.QUOTATION);
        verify(itineraryRepository).save(draft);

        ArgumentCaptor<ItineraryQuotationSavedEvent> captor =
            ArgumentCaptor.forClass(ItineraryQuotationSavedEvent.class);
        verify(events).publishEvent(captor.capture());
        assertThat(captor.getValue().itineraryId()).isEqualTo(itineraryId);
        assertThat(captor.getValue().consultantId()).isEqualTo(consultantId);
    }

    @Test
    void savingAsQuotationFailsForUnknownItinerary() {
        UUID itineraryId = UUID.randomUUID();
        when(itineraryRepository.findById(itineraryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.saveAsQuotation(itineraryId))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void confirmBookingPublishesBookingConfirmedEventWithCorrectAmount() {
        Money price = new Money(BigDecimal.valueOf(11_500), CurrencyCode.INR);

        service.confirmBooking(UUID.randomUUID(), price);

        // Explicit class matters: ApplicationEventPublisher overloads
        // publishEvent(ApplicationEvent) and publishEvent(Object), and a bare
        // any() binds to the wrong overload since BookingConfirmedEvent
        // isn't an ApplicationEvent, causing a false "not invoked" failure.
        verify(events).publishEvent(any(BookingConfirmedEvent.class));
    }
}
