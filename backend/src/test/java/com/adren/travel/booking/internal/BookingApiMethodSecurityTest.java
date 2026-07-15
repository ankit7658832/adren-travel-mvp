package com.adren.travel.booking.internal;

import com.adren.travel.booking.BookingApi;
import com.adren.travel.payments.PaymentsApi;
import com.adren.travel.security.AdrenPrincipal;
import com.adren.travel.security.Role;
import com.adren.travel.supplier.SupplierSearchApi;
import com.adren.travel.whitelabel.WhitelabelApi;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Proves FND-02's actual acceptance criterion — the {@code @PreAuthorize}
 * expressions on {@link BookingApi} are enforced by a real Spring AOP
 * proxy, not just present as annotations nobody evaluates — against a
 * minimal, DB-free Spring context (mocked repository/publisher) rather
 * than a full {@code @ApplicationModuleTest}, since this story is about the
 * authorization decision, not the persistence behind it.
 */
class BookingApiMethodSecurityTest {

    @Configuration
    @EnableMethodSecurity
    static class TestConfig {
        @Bean
        ItineraryRepository itineraryRepository() {
            return Mockito.mock(ItineraryRepository.class);
        }

        @Bean
        TravelerProfileRepository travelerProfileRepository() {
            return Mockito.mock(TravelerProfileRepository.class);
        }

        @Bean
        ApplicationEventPublisher applicationEventPublisher() {
            return Mockito.mock(ApplicationEventPublisher.class);
        }

        @Bean
        WhitelabelApi whitelabelApi() {
            return Mockito.mock(WhitelabelApi.class);
        }

        @Bean
        SupplierSearchApi supplierSearchApi() {
            return Mockito.mock(SupplierSearchApi.class);
        }

        @Bean
        HotelLineItemRepository hotelLineItemRepository() {
            return Mockito.mock(HotelLineItemRepository.class);
        }

        @Bean
        QuotationRepository quotationRepository() {
            return Mockito.mock(QuotationRepository.class);
        }

        @Bean
        PaymentsApi paymentsApi() {
            return Mockito.mock(PaymentsApi.class);
        }

        @Bean
        BookingApi bookingApi(ItineraryRepository repository, TravelerProfileRepository travelerProfileRepository,
                               HotelLineItemRepository hotelLineItemRepository, QuotationRepository quotationRepository,
                               ApplicationEventPublisher publisher, WhitelabelApi whitelabelApi,
                               SupplierSearchApi supplierSearchApi, PaymentsApi paymentsApi) {
            return new BookingServiceImpl(repository, travelerProfileRepository, hotelLineItemRepository,
                quotationRepository, publisher, whitelabelApi, supplierSearchApi, paymentsApi);
        }
    }

    private static AnnotationConfigApplicationContext context;
    private static BookingApi bookingApi;

    @BeforeAll
    static void startContext() {
        context = new AnnotationConfigApplicationContext(TestConfig.class);
        bookingApi = context.getBean(BookingApi.class);
    }

    @AfterAll
    static void stopContext() {
        context.close();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void rejectsAnUnauthenticatedCallerBeforeReachingTheServiceMethod() {
        UUID itineraryId = UUID.randomUUID();

        assertThatThrownBy(() -> bookingApi.saveAsQuotation(itineraryId))
            .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
    }

    @Test
    void allowsAConsultantPrincipalToSaveAsQuotation() {
        UUID itineraryId = UUID.randomUUID();
        UUID consultantId = UUID.randomUUID();
        ItineraryRepository repository = context.getBean(ItineraryRepository.class);
        Itinerary draft = new Itinerary(itineraryId, consultantId, null);
        when(repository.findById(itineraryId)).thenReturn(Optional.of(draft));
        stubExistingHotelLineItem(itineraryId);

        authenticateAs(Role.CONSULTANT, consultantId);

        assertThat(bookingApi.saveAsQuotation(itineraryId)).isNotNull();
    }

    @Test
    void allowsAUserPrincipalToSaveAsQuotation() {
        UUID itineraryId = UUID.randomUUID();
        UUID consultantId = UUID.randomUUID();
        ItineraryRepository repository = context.getBean(ItineraryRepository.class);
        Itinerary draft = new Itinerary(itineraryId, consultantId, null);
        when(repository.findById(itineraryId)).thenReturn(Optional.of(draft));
        stubExistingHotelLineItem(itineraryId);

        authenticateAs(Role.USER, consultantId);

        assertThat(bookingApi.saveAsQuotation(itineraryId)).isNotNull();
    }

    @Test
    void fnd03ConsultantCannotSaveAnotherConsultantsItineraryThroughTheRealApiProxy() {
        UUID itineraryId = UUID.randomUUID();
        UUID ownerConsultantId = UUID.randomUUID();
        UUID otherConsultantId = UUID.randomUUID();
        ItineraryRepository repository = context.getBean(ItineraryRepository.class);
        Itinerary draft = new Itinerary(itineraryId, ownerConsultantId, null);
        when(repository.findById(itineraryId)).thenReturn(Optional.of(draft));

        authenticateAs(Role.CONSULTANT, otherConsultantId);

        assertThatThrownBy(() -> bookingApi.saveAsQuotation(itineraryId))
            .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }

    @Test
    void findBookingsByConsultantIsAlsoGuardedByMethodSecurity() {
        ItineraryRepository repository = context.getBean(ItineraryRepository.class);
        UUID consultantId = UUID.randomUUID();
        when(repository.findByConsultantId(Mockito.eq(consultantId), Mockito.any()))
            .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of()));

        assertThatThrownBy(() -> bookingApi.findBookingsByConsultant(consultantId, PageRequest.of(0, 20)))
            .isInstanceOf(AuthenticationCredentialsNotFoundException.class);

        authenticateAs(Role.SUPER_ADMIN, null);
        assertThat(bookingApi.findBookingsByConsultant(consultantId, PageRequest.of(0, 20)).getContent()).isEmpty();
    }

    private static void stubExistingHotelLineItem(UUID itineraryId) {
        HotelLineItemRepository hotelLineItemRepository = context.getBean(HotelLineItemRepository.class);
        HotelLineItem existing = new HotelLineItem(UUID.randomUUID(), itineraryId,
            com.adren.travel.supplier.SupplierId.HOTELBEDS, "rate-key-1", "Taj Palace", "Deluxe Room",
            com.adren.travel.booking.MealPlan.BB, java.time.Instant.now().plusSeconds(3600),
            java.math.BigDecimal.valueOf(100), com.adren.travel.shared.CurrencyCode.INR,
            java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO, java.math.BigDecimal.valueOf(100),
            com.adren.travel.shared.CurrencyCode.INR, java.math.BigDecimal.ONE);
        when(hotelLineItemRepository.findByItineraryId(itineraryId)).thenReturn(List.of(existing));
    }

    private static void authenticateAs(Role role, UUID consultantId) {
        AdrenPrincipal principal = new AdrenPrincipal(UUID.randomUUID(), role, consultantId);
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
        var authentication = new UsernamePasswordAuthenticationToken(principal, null, authorities);
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
    }
}
