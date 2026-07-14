package com.adren.travel.booking;

import com.adren.travel.booking.event.BookingConfirmedEvent;
import com.adren.travel.shared.CurrencyCode;
import com.adren.travel.shared.Money;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.Scenario;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * {@code @ApplicationModuleTest} boots ONLY the Booking module (plus the
 * shared/open modules it depends on) — not the whole application context.
 * This is the Spring Modulith equivalent of a "slice test": faster than
 * {@code @SpringBootTest}, but still exercises real Spring wiring and a
 * real (embedded/test) database, unlike a pure Mockito unit test.
 * <p>
 * {@link Scenario} lets you assert on published events without manually
 * wiring a test listener — this is the standard way to verify the
 * event-driven contract PRD Section 15 depends on.
 * <p>
 * {@code DIRECT_DEPENDENCIES} (rather than the default {@code STANDALONE},
 * which boots only this module) is required since FND-13:
 * {@code GeocodeAndSearchService} has a real constructor dependency on
 * {@code supplier.SupplierSearchApi}.
 */
@ApplicationModuleTest(ApplicationModuleTest.BootstrapMode.DIRECT_DEPENDENCIES)
class BookingModuleIntegrationTests {

    /**
     * {@code @ApplicationModuleTest}'s slice doesn't auto-configure
     * {@code WebClient.Builder} the way a full {@code @SpringBootTest}
     * would — {@code supplier}'s {@code HotelbedsClient} (now reachable
     * transitively via FND-13's {@code GeocodeAndSearchService}) needs one.
     */
    @TestConfiguration
    static class WebClientTestConfig {
        @Bean
        WebClient.Builder webClientBuilder() {
            return WebClient.builder();
        }
    }

    @Autowired
    BookingApi bookingApi;

    @Test
    void confirmingABookingPublishesBookingConfirmedEvent(Scenario scenario) {
        UUID packageId = UUID.randomUUID();
        Money price = new Money(BigDecimal.valueOf(11_500), CurrencyCode.INR);

        scenario.stimulate(() -> bookingApi.confirmBooking(packageId, price))
            .andWaitForEventOfType(BookingConfirmedEvent.class)
            .matchingMappedValue(BookingConfirmedEvent::totalSellPrice, price.amount());
    }
}
