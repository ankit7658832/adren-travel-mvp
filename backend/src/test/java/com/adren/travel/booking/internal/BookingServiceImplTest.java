package com.adren.travel.booking.internal;

import com.adren.travel.booking.AlternateOption;
import com.adren.travel.booking.event.BookingConfirmedEvent;
import com.adren.travel.booking.event.ItineraryQuotationSavedEvent;
import com.adren.travel.security.AdrenPrincipal;
import com.adren.travel.security.Role;
import com.adren.travel.shared.CurrencyCode;
import com.adren.travel.shared.Money;
import com.adren.travel.supplier.SupplierId;
import com.adren.travel.supplier.SupplierSearchApi;
import com.adren.travel.supplier.SupplierSearchResult;
import com.adren.travel.whitelabel.WhitelabelApi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
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

    @Mock
    WhitelabelApi whitelabelApi;

    @Mock
    SupplierSearchApi supplierSearchApi;

    BookingServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new BookingServiceImpl(itineraryRepository, events, whitelabelApi, supplierSearchApi);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void savingAsQuotationTransitionsStatusAndPublishesEvent() {
        UUID itineraryId = UUID.randomUUID();
        UUID consultantId = UUID.randomUUID();
        Itinerary draft = new Itinerary(itineraryId, consultantId, null);
        when(itineraryRepository.findById(itineraryId)).thenReturn(Optional.of(draft));
        authenticateAs(Role.CONSULTANT, consultantId);

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
        authenticateAs(Role.SUPER_ADMIN, null);

        assertThatThrownBy(() -> service.saveAsQuotation(itineraryId))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aConsultantCannotSaveAnotherConsultantsItineraryAsQuotationFND03() {
        UUID itineraryId = UUID.randomUUID();
        UUID ownerConsultantId = UUID.randomUUID();
        UUID otherConsultantId = UUID.randomUUID();
        Itinerary draft = new Itinerary(itineraryId, ownerConsultantId, null);
        when(itineraryRepository.findById(itineraryId)).thenReturn(Optional.of(draft));
        authenticateAs(Role.CONSULTANT, otherConsultantId);

        assertThatThrownBy(() -> service.saveAsQuotation(itineraryId))
            .isInstanceOf(AccessDeniedException.class);
        assertThat(draft.getStatus()).isEqualTo(ItineraryStatus.DRAFT);
    }

    @Test
    void aUserUnderTheOwningConsultantCanSaveItAsQuotation() {
        UUID itineraryId = UUID.randomUUID();
        UUID consultantId = UUID.randomUUID();
        Itinerary draft = new Itinerary(itineraryId, consultantId, null);
        when(itineraryRepository.findById(itineraryId)).thenReturn(Optional.of(draft));
        authenticateAs(Role.USER, consultantId);

        service.saveAsQuotation(itineraryId);

        assertThat(draft.getStatus()).isEqualTo(ItineraryStatus.QUOTATION);
    }

    @Test
    void savingAsQuotationRejectsASuspendedConsultantsUserFND05() {
        UUID itineraryId = UUID.randomUUID();
        UUID consultantId = UUID.randomUUID();
        Itinerary draft = new Itinerary(itineraryId, consultantId, null);
        when(itineraryRepository.findById(itineraryId)).thenReturn(Optional.of(draft));
        authenticateAs(Role.CONSULTANT, consultantId);
        org.mockito.Mockito.doThrow(new AccessDeniedException("suspended"))
            .when(whitelabelApi).requireConsultantActive(consultantId);

        assertThatThrownBy(() -> service.saveAsQuotation(itineraryId))
            .isInstanceOf(AccessDeniedException.class);
        assertThat(draft.getStatus()).isEqualTo(ItineraryStatus.DRAFT);
    }

    @Test
    void aSuperAdminCanSaveAnyConsultantsItineraryAsQuotation() {
        UUID itineraryId = UUID.randomUUID();
        Itinerary draft = new Itinerary(itineraryId, UUID.randomUUID(), null);
        when(itineraryRepository.findById(itineraryId)).thenReturn(Optional.of(draft));
        authenticateAs(Role.SUPER_ADMIN, null);

        service.saveAsQuotation(itineraryId);

        assertThat(draft.getStatus()).isEqualTo(ItineraryStatus.QUOTATION);
    }

    @Test
    void confirmBookingPublishesBookingConfirmedEventWithCorrectAmount() {
        Money price = new Money(BigDecimal.valueOf(11_500), CurrencyCode.INR);
        authenticateAs(Role.CONSULTANT, UUID.randomUUID());

        service.confirmBooking(UUID.randomUUID(), price);

        // Explicit class matters: ApplicationEventPublisher overloads
        // publishEvent(ApplicationEvent) and publishEvent(Object), and a bare
        // any() binds to the wrong overload since BookingConfirmedEvent
        // isn't an ApplicationEvent, causing a false "not invoked" failure.
        verify(events).publishEvent(any(BookingConfirmedEvent.class));
    }

    @Test
    void confirmBookingRejectsASuspendedConsultantsUserFND05() {
        UUID consultantId = UUID.randomUUID();
        Money price = new Money(BigDecimal.valueOf(1000), CurrencyCode.INR);
        authenticateAs(Role.USER, consultantId);
        org.mockito.Mockito.doThrow(new AccessDeniedException("suspended"))
            .when(whitelabelApi).requireConsultantActive(consultantId);

        assertThatThrownBy(() -> service.confirmBooking(UUID.randomUUID(), price))
            .isInstanceOf(AccessDeniedException.class);
        verify(events, org.mockito.Mockito.never()).publishEvent(any(BookingConfirmedEvent.class));
    }

    @Test
    void confirmBookingSkipsTheActiveGateForASuperAdmin() {
        Money price = new Money(BigDecimal.valueOf(1000), CurrencyCode.INR);
        authenticateAs(Role.SUPER_ADMIN, null);

        service.confirmBooking(UUID.randomUUID(), price);

        verify(whitelabelApi, org.mockito.Mockito.never()).requireConsultantActive(any());
    }

    @Test
    void findAlternatesReturnsEveryHotelOptionForTheLocationFND16() {
        authenticateAs(Role.CONSULTANT, UUID.randomUUID());
        LocalDate checkIn = LocalDate.now().plusDays(30);
        LocalDate checkOut = checkIn.plusDays(3);
        when(supplierSearchApi.searchHotels("Goa", checkIn, checkOut)).thenReturn(List.of(
            new SupplierSearchResult(SupplierId.HOTELBEDS, "rate-1", "Hotel A", "Deluxe",
                new Money(BigDecimal.valueOf(5000), CurrencyCode.INR), 4.2)));

        List<AlternateOption> alternates =
            service.findAlternates(UUID.randomUUID(), "Goa", "hotel", checkIn, checkOut);

        assertThat(alternates).hasSize(1);
        assertThat(alternates.get(0).supplierId()).isEqualTo("HOTELBEDS");
        assertThat(alternates.get(0).supplierRateId()).isEqualTo("rate-1");
        assertThat(alternates.get(0).netRateAmount()).isEqualByComparingTo("5000.00");
        assertThat(alternates.get(0).netRateCurrency()).isEqualTo(CurrencyCode.INR);
        assertThat(alternates.get(0).rating()).isEqualTo(4.2);
    }

    @Test
    void findAlternatesReturnsEmptyForANonHotelCategoryFND16() {
        authenticateAs(Role.CONSULTANT, UUID.randomUUID());
        LocalDate checkIn = LocalDate.now().plusDays(30);

        List<AlternateOption> alternates =
            service.findAlternates(UUID.randomUUID(), "Goa", "flight", checkIn, checkIn.plusDays(3));

        assertThat(alternates).isEmpty();
    }

    @Test
    void findAlternatesRejectsASuspendedConsultantsUserFND16() {
        UUID consultantId = UUID.randomUUID();
        authenticateAs(Role.USER, consultantId);
        org.mockito.Mockito.doThrow(new AccessDeniedException("suspended"))
            .when(whitelabelApi).requireConsultantActive(consultantId);
        LocalDate checkIn = LocalDate.now().plusDays(30);

        assertThatThrownBy(() -> service.findAlternates(UUID.randomUUID(), "Goa", "hotel", checkIn, checkIn.plusDays(3)))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void findBookingsByConsultantReturnsAPageOfItineraryIds() {
        UUID consultantId = UUID.randomUUID();
        Itinerary itinerary = new Itinerary(UUID.randomUUID(), consultantId, null);
        Pageable pageable = PageRequest.of(0, 20);
        Page<Itinerary> page = new PageImpl<>(List.of(itinerary), pageable, 1);
        when(itineraryRepository.findByConsultantId(consultantId, pageable)).thenReturn(page);
        authenticateAs(Role.CONSULTANT, consultantId);

        Page<UUID> result = service.findBookingsByConsultant(consultantId, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).containsExactly(itinerary.getItineraryId());
    }

    @Test
    void aConsultantCannotListAnotherConsultantsBookingsFND03() {
        UUID ownConsultantId = UUID.randomUUID();
        UUID otherConsultantId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 20);
        authenticateAs(Role.CONSULTANT, ownConsultantId);

        assertThatThrownBy(() -> service.findBookingsByConsultant(otherConsultantId, pageable))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void aSuperAdminCanListAnyConsultantsBookingsViaTheExplicitViewAllPath() {
        UUID consultantId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 20);
        when(itineraryRepository.findByConsultantId(consultantId, pageable))
            .thenReturn(new PageImpl<>(List.of()));
        authenticateAs(Role.SUPER_ADMIN, null);

        assertThat(service.findBookingsByConsultant(consultantId, pageable).getContent()).isEmpty();
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
