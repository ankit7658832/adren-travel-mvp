package com.adren.travel.booking;

import com.adren.travel.booking.event.BookingConfirmedEvent;
import com.adren.travel.security.AdrenPrincipal;
import com.adren.travel.security.Role;
import com.adren.travel.shared.CurrencyCode;
import com.adren.travel.shared.Money;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.Scenario;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.util.List;
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

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void confirmingABookingPublishesBookingConfirmedEvent(Scenario scenario) {
        UUID packageId = UUID.randomUUID();
        Money price = new Money(BigDecimal.valueOf(11_500), CurrencyCode.INR);
        // FND-05's tenant-active gate is exercised in BookingServiceImplTest;
        // authenticate as SUPER_ADMIN here (no consultantId, gate skipped)
        // so this test stays focused on the event-publication contract.
        authenticateAsSuperAdmin();

        scenario.stimulate(() -> bookingApi.confirmBooking(packageId, price))
            .andWaitForEventOfType(BookingConfirmedEvent.class)
            .matchingMappedValue(BookingConfirmedEvent::totalSellPrice, price.amount());
    }

    private static void authenticateAsSuperAdmin() {
        AdrenPrincipal principal = new AdrenPrincipal(UUID.randomUUID(), Role.SUPER_ADMIN, null);
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"));
        var authentication = new UsernamePasswordAuthenticationToken(principal, null, authorities);
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
    }
}
