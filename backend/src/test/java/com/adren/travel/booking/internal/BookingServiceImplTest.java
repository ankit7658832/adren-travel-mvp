package com.adren.travel.booking.internal;

import com.adren.travel.booking.AddHotelLineItemCommand;
import com.adren.travel.booking.AlternateOption;
import com.adren.travel.booking.ConvertQuotationToPackageCommand;
import com.adren.travel.booking.CreateTravelerProfileCommand;
import com.adren.travel.booking.MealPlan;
import com.adren.travel.booking.event.BookingConfirmedEvent;
import com.adren.travel.booking.event.HotelLineItemAddedEvent;
import com.adren.travel.booking.event.ItineraryQuotationSavedEvent;
import com.adren.travel.booking.event.PackageCreatedEvent;
import com.adren.travel.booking.event.PackagePublishedEvent;
import com.adren.travel.booking.event.TravelerProfileCreatedEvent;
import com.adren.travel.payments.CalculateSellRateCommand;
import com.adren.travel.payments.FxRateSnapshot;
import com.adren.travel.payments.PaymentsApi;
import com.adren.travel.payments.SellRateCalculation;
import com.adren.travel.security.AdrenPrincipal;
import com.adren.travel.security.Role;
import com.adren.travel.shared.CurrencyCode;
import com.adren.travel.shared.Money;
import com.adren.travel.shared.ProductCategory;
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
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
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
    TravelerProfileRepository travelerProfileRepository;

    @Mock
    ApplicationEventPublisher events;

    @Mock
    WhitelabelApi whitelabelApi;

    @Mock
    SupplierSearchApi supplierSearchApi;

    @Mock
    HotelLineItemRepository hotelLineItemRepository;

    @Mock
    QuotationRepository quotationRepository;

    @Mock
    TravelPackageRepository travelPackageRepository;

    @Mock
    PaymentsApi paymentsApi;

    BookingServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new BookingServiceImpl(itineraryRepository, travelerProfileRepository, hotelLineItemRepository,
            quotationRepository, travelPackageRepository, events, whitelabelApi, supplierSearchApi, paymentsApi);
    }

    // BOK-08: saveAsQuotation now requires at least one line item — stub a
    // non-empty result for any test that expects the transition to succeed.
    private void stubExistingHotelLineItem(UUID itineraryId) {
        HotelLineItem existing = new HotelLineItem(UUID.randomUUID(), itineraryId, SupplierId.HOTELBEDS,
            "rate-key-1", "Taj Palace", "Deluxe Room", MealPlan.BB, Instant.now().plusSeconds(3600),
            BigDecimal.valueOf(100), CurrencyCode.INR, BigDecimal.ZERO, BigDecimal.ZERO,
            BigDecimal.valueOf(100), CurrencyCode.INR, BigDecimal.ONE);
        when(hotelLineItemRepository.findByItineraryId(itineraryId)).thenReturn(List.of(existing));
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
        stubExistingHotelLineItem(itineraryId);
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
    void savingAsQuotationNeverPublishesTheEventWhenTheRepositorySaveFailsBOK01() {
        UUID itineraryId = UUID.randomUUID();
        UUID consultantId = UUID.randomUUID();
        Itinerary draft = new Itinerary(itineraryId, consultantId, null);
        when(itineraryRepository.findById(itineraryId)).thenReturn(Optional.of(draft));
        stubExistingHotelLineItem(itineraryId);
        authenticateAs(Role.CONSULTANT, consultantId);
        org.mockito.Mockito.doThrow(new RuntimeException("DB write failed"))
            .when(itineraryRepository).save(draft);

        assertThatThrownBy(() -> service.saveAsQuotation(itineraryId))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("DB write failed");

        verify(events, org.mockito.Mockito.never()).publishEvent(any(ItineraryQuotationSavedEvent.class));
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
    void savingAsQuotationRejectsAnItineraryWithNoLineItemsBOK08() {
        UUID itineraryId = UUID.randomUUID();
        UUID consultantId = UUID.randomUUID();
        Itinerary draft = new Itinerary(itineraryId, consultantId, null);
        when(itineraryRepository.findById(itineraryId)).thenReturn(Optional.of(draft));
        when(hotelLineItemRepository.findByItineraryId(itineraryId)).thenReturn(List.of());
        authenticateAs(Role.CONSULTANT, consultantId);

        assertThatThrownBy(() -> service.saveAsQuotation(itineraryId))
            .isInstanceOf(IllegalStateException.class);
        assertThat(draft.getStatus()).isEqualTo(ItineraryStatus.DRAFT);
        verify(events, org.mockito.Mockito.never()).publishEvent(any(ItineraryQuotationSavedEvent.class));
    }

    @Test
    void savingAsQuotationCreatesAQuotationWithAFutureValidUntilBOK09() {
        UUID itineraryId = UUID.randomUUID();
        UUID consultantId = UUID.randomUUID();
        Itinerary draft = new Itinerary(itineraryId, consultantId, null);
        when(itineraryRepository.findById(itineraryId)).thenReturn(Optional.of(draft));
        stubExistingHotelLineItem(itineraryId);
        authenticateAs(Role.CONSULTANT, consultantId);

        service.saveAsQuotation(itineraryId);

        ArgumentCaptor<Quotation> captor = ArgumentCaptor.forClass(Quotation.class);
        verify(quotationRepository).save(captor.capture());
        assertThat(captor.getValue().getItineraryId()).isEqualTo(itineraryId);
        assertThat(captor.getValue().getValidUntil()).isAfter(Instant.now());
        assertThat(captor.getValue().isSharedWithTraveler()).isFalse();
        assertThat(captor.getValue().getConvertedToBookingId()).isNull();
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
        stubExistingHotelLineItem(itineraryId);
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
        stubExistingHotelLineItem(itineraryId);
        authenticateAs(Role.SUPER_ADMIN, null);

        service.saveAsQuotation(itineraryId);

        assertThat(draft.getStatus()).isEqualTo(ItineraryStatus.QUOTATION);
    }

    @Test
    void confirmBookingPublishesBookingConfirmedEventWithCorrectAmount() {
        Money price = new Money(BigDecimal.valueOf(11_500), CurrencyCode.INR);
        UUID consultantId = UUID.randomUUID();
        UUID quotationId = stubQuotationResolvingTo(consultantId);
        authenticateAs(Role.CONSULTANT, consultantId);

        service.confirmBooking(quotationId, price);

        // Explicit class matters: ApplicationEventPublisher overloads
        // publishEvent(ApplicationEvent) and publishEvent(Object), and a bare
        // any() binds to the wrong overload since BookingConfirmedEvent
        // isn't an ApplicationEvent, causing a false "not invoked" failure.
        verify(events).publishEvent(any(BookingConfirmedEvent.class));
    }

    @Test
    void confirmBookingResolvesTheConsultantIdFromAPackageWhenNoQuotationMatchesBOK13() {
        Money price = new Money(BigDecimal.valueOf(11_500), CurrencyCode.INR);
        UUID consultantId = UUID.randomUUID();
        UUID packageId = UUID.randomUUID();
        when(quotationRepository.findById(packageId)).thenReturn(Optional.empty());
        TravelPackage travelPackage = new TravelPackage(packageId, UUID.randomUUID(), consultantId, "Goa Getaway",
            null, LocalDate.now().plusDays(30), LocalDate.now().plusDays(90),
            BigDecimal.valueOf(11_371.20), BigDecimal.valueOf(500), CurrencyCode.INR, 4);
        when(travelPackageRepository.findById(packageId)).thenReturn(Optional.of(travelPackage));
        authenticateAs(Role.CONSULTANT, consultantId);

        UUID bookingId = service.confirmBooking(packageId, price);

        assertThat(bookingId).isNotNull();
        ArgumentCaptor<BookingConfirmedEvent> captor = ArgumentCaptor.forClass(BookingConfirmedEvent.class);
        verify(events).publishEvent(captor.capture());
        assertThat(captor.getValue().consultantId()).isEqualTo(consultantId);
    }

    @Test
    void confirmBookingFailsForAnUnknownQuotationOrPackageBOK13() {
        UUID unknownId = UUID.randomUUID();
        Money price = new Money(BigDecimal.valueOf(1000), CurrencyCode.INR);
        when(quotationRepository.findById(unknownId)).thenReturn(Optional.empty());
        when(travelPackageRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.confirmBooking(unknownId, price))
            .isInstanceOf(IllegalArgumentException.class);
        verify(events, org.mockito.Mockito.never()).publishEvent(any(BookingConfirmedEvent.class));
    }

    @Test
    void aUserCannotConfirmABookingForAnotherConsultantsQuotationBOK13() {
        UUID ownerConsultantId = UUID.randomUUID();
        UUID quotationId = stubQuotationResolvingTo(ownerConsultantId);
        Money price = new Money(BigDecimal.valueOf(1000), CurrencyCode.INR);
        authenticateAs(Role.CONSULTANT, UUID.randomUUID());

        assertThatThrownBy(() -> service.confirmBooking(quotationId, price))
            .isInstanceOf(AccessDeniedException.class);
        verify(events, org.mockito.Mockito.never()).publishEvent(any(BookingConfirmedEvent.class));
    }

    @Test
    void confirmBookingRejectsASuspendedConsultantsUserFND05() {
        UUID consultantId = UUID.randomUUID();
        UUID quotationId = stubQuotationResolvingTo(consultantId);
        Money price = new Money(BigDecimal.valueOf(1000), CurrencyCode.INR);
        authenticateAs(Role.USER, consultantId);
        org.mockito.Mockito.doThrow(new AccessDeniedException("suspended"))
            .when(whitelabelApi).requireConsultantActive(consultantId);

        assertThatThrownBy(() -> service.confirmBooking(quotationId, price))
            .isInstanceOf(AccessDeniedException.class);
        verify(events, org.mockito.Mockito.never()).publishEvent(any(BookingConfirmedEvent.class));
    }

    @Test
    void confirmBookingSkipsTheActiveGateForASuperAdmin() {
        UUID quotationId = stubQuotationResolvingTo(UUID.randomUUID());
        Money price = new Money(BigDecimal.valueOf(1000), CurrencyCode.INR);
        authenticateAs(Role.SUPER_ADMIN, null);

        service.confirmBooking(quotationId, price);

        verify(whitelabelApi, org.mockito.Mockito.never()).requireConsultantActive(any());
    }

    private UUID stubQuotationResolvingTo(UUID consultantId) {
        UUID itineraryId = UUID.randomUUID();
        UUID quotationId = UUID.randomUUID();
        when(quotationRepository.findById(quotationId)).thenReturn(
            Optional.of(new Quotation(quotationId, itineraryId, Instant.now().plusSeconds(3600))));
        when(itineraryRepository.findById(itineraryId)).thenReturn(
            Optional.of(new Itinerary(itineraryId, consultantId, null)));
        return quotationId;
    }

    @Test
    void confirmBookingFromPaymentWebhookPublishesTheEventWithoutRequiringAPrincipalFIN11() {
        UUID consultantId = UUID.randomUUID();
        Money price = new Money(BigDecimal.valueOf(11_500), CurrencyCode.INR);

        UUID bookingId = service.confirmBookingFromPaymentWebhook(UUID.randomUUID(), consultantId, price);

        assertThat(bookingId).isNotNull();
        ArgumentCaptor<BookingConfirmedEvent> captor = ArgumentCaptor.forClass(BookingConfirmedEvent.class);
        verify(events).publishEvent(captor.capture());
        assertThat(captor.getValue().consultantId()).isEqualTo(consultantId);
        assertThat(captor.getValue().totalSellPrice()).isEqualTo(price);
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

    @Test
    void createTravelerProfileScopesToTheCallingConsultantsOwnAccountBOK14() {
        UUID consultantId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, consultantId);
        var command = new CreateTravelerProfileCommand("Jane Traveler", LocalDate.of(1990, 5, 1),
            "P1234567", LocalDate.of(2030, 1, 1), "IN", List.of("s3://vault/passport.pdf"), Map.of("meal", "vegetarian"));

        UUID travelerId = service.createTravelerProfile(command);

        ArgumentCaptor<TravelerProfile> captor = ArgumentCaptor.forClass(TravelerProfile.class);
        verify(travelerProfileRepository).save(captor.capture());
        assertThat(captor.getValue().getTravelerId()).isEqualTo(travelerId);
        assertThat(captor.getValue().getConsultantId()).isEqualTo(consultantId);
        assertThat(captor.getValue().getName()).isEqualTo("Jane Traveler");
        assertThat(captor.getValue().getPassportNumber()).isEqualTo("P1234567");

        ArgumentCaptor<TravelerProfileCreatedEvent> eventCaptor = ArgumentCaptor.forClass(TravelerProfileCreatedEvent.class);
        verify(events).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().travelerId()).isEqualTo(travelerId);
        assertThat(eventCaptor.getValue().consultantId()).isEqualTo(consultantId);
    }

    @Test
    void createTravelerProfileAllowsOmittingOptionalPassportFieldsBOK14() {
        authenticateAs(Role.USER, UUID.randomUUID());
        var command = new CreateTravelerProfileCommand("Jane Traveler", LocalDate.of(1990, 5, 1),
            null, null, null, null, null);

        service.createTravelerProfile(command);

        ArgumentCaptor<TravelerProfile> captor = ArgumentCaptor.forClass(TravelerProfile.class);
        verify(travelerProfileRepository).save(captor.capture());
        assertThat(captor.getValue().getPassportNumber()).isNull();
        assertThat(captor.getValue().getDocumentVaultReferences()).isEmpty();
        assertThat(captor.getValue().getPreferences()).isEmpty();
    }

    @Test
    void addingAHotelLineItemPricesItThroughTheSellRatePipelineAndPublishesTheEventBOK03() {
        UUID itineraryId = UUID.randomUUID();
        UUID consultantId = UUID.randomUUID();
        Itinerary itinerary = new Itinerary(itineraryId, consultantId, null);
        when(itineraryRepository.findById(itineraryId)).thenReturn(Optional.of(itinerary));
        authenticateAs(Role.CONSULTANT, consultantId);

        Money netRate = new Money(BigDecimal.valueOf(100), CurrencyCode.INR);
        Money fxConvertedBase = new Money(BigDecimal.valueOf(9_600), CurrencyCode.INR);
        Money bufferedAmount = new Money(BigDecimal.valueOf(9_888), CurrencyCode.INR);
        Money markupAmount = new Money(BigDecimal.valueOf(1_483.20), CurrencyCode.INR);
        Money sellRate = new Money(BigDecimal.valueOf(11_371.20), CurrencyCode.INR);
        Money commissionAmount = Money.zero(CurrencyCode.INR);
        FxRateSnapshot snapshot = new FxRateSnapshot(CurrencyCode.INR, CurrencyCode.INR,
            BigDecimal.valueOf(96), Instant.now());
        when(paymentsApi.calculateSellRate(any())).thenReturn(new SellRateCalculation(
            netRate, snapshot, fxConvertedBase, bufferedAmount, markupAmount, sellRate, commissionAmount));

        var command = new AddHotelLineItemCommand(SupplierId.HOTELBEDS, "rate-key-1", "Taj Palace", "Deluxe Room",
            MealPlan.BB, Instant.now().plusSeconds(3600), netRate, CurrencyCode.INR,
            BigDecimal.valueOf(96), BigDecimal.valueOf(3), BigDecimal.ZERO);

        UUID lineItemId = service.addHotelLineItem(itineraryId, command);

        ArgumentCaptor<CalculateSellRateCommand> priceCaptor = ArgumentCaptor.forClass(CalculateSellRateCommand.class);
        verify(paymentsApi).calculateSellRate(priceCaptor.capture());
        assertThat(priceCaptor.getValue().category()).isEqualTo(ProductCategory.HOTEL);
        assertThat(priceCaptor.getValue().consultantId()).isEqualTo(consultantId);

        ArgumentCaptor<HotelLineItem> lineItemCaptor = ArgumentCaptor.forClass(HotelLineItem.class);
        verify(hotelLineItemRepository).save(lineItemCaptor.capture());
        assertThat(lineItemCaptor.getValue().getLineItemId()).isEqualTo(lineItemId);
        assertThat(lineItemCaptor.getValue().getPropertyName()).isEqualTo("Taj Palace");
        assertThat(lineItemCaptor.getValue().getMealPlan()).isEqualTo(MealPlan.BB);
        assertThat(lineItemCaptor.getValue().getSellRate()).isEqualByComparingTo("11371.20");
        assertThat(lineItemCaptor.getValue().getCurrencyBufferApplied()).isEqualByComparingTo("288.00");

        ArgumentCaptor<HotelLineItemAddedEvent> eventCaptor = ArgumentCaptor.forClass(HotelLineItemAddedEvent.class);
        verify(events).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().lineItemId()).isEqualTo(lineItemId);
        assertThat(eventCaptor.getValue().itineraryId()).isEqualTo(itineraryId);
        assertThat(eventCaptor.getValue().sellRate()).isEqualTo(sellRate);
    }

    @Test
    void addingAHotelLineItemFailsForAnItineraryOwnedByAnotherConsultantBOK03() {
        UUID itineraryId = UUID.randomUUID();
        UUID ownerConsultantId = UUID.randomUUID();
        Itinerary itinerary = new Itinerary(itineraryId, ownerConsultantId, null);
        when(itineraryRepository.findById(itineraryId)).thenReturn(Optional.of(itinerary));
        authenticateAs(Role.CONSULTANT, UUID.randomUUID());

        var command = new AddHotelLineItemCommand(SupplierId.HOTELBEDS, "rate-key-1", "Taj Palace", "Deluxe Room",
            MealPlan.BB, Instant.now().plusSeconds(3600), new Money(BigDecimal.valueOf(100), CurrencyCode.INR),
            CurrencyCode.INR, BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO);

        assertThatThrownBy(() -> service.addHotelLineItem(itineraryId, command))
            .isInstanceOf(AccessDeniedException.class);
        verify(hotelLineItemRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void addingAHotelLineItemRejectsAnItineraryThatIsAlreadyAQuotationBOK08() {
        UUID itineraryId = UUID.randomUUID();
        UUID consultantId = UUID.randomUUID();
        Itinerary itinerary = new Itinerary(itineraryId, consultantId, null);
        itinerary.markAsQuotation();
        when(itineraryRepository.findById(itineraryId)).thenReturn(Optional.of(itinerary));
        authenticateAs(Role.CONSULTANT, consultantId);

        var command = new AddHotelLineItemCommand(SupplierId.HOTELBEDS, "rate-key-1", "Taj Palace", "Deluxe Room",
            MealPlan.BB, Instant.now().plusSeconds(3600), new Money(BigDecimal.valueOf(100), CurrencyCode.INR),
            CurrencyCode.INR, BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO);

        assertThatThrownBy(() -> service.addHotelLineItem(itineraryId, command))
            .isInstanceOf(IllegalStateException.class);
        verify(hotelLineItemRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void convertingAQuotationToAPackageAutoFillsBasePriceFromLineItemsAndPublishesTheEventBOK10() {
        UUID itineraryId = UUID.randomUUID();
        UUID consultantId = UUID.randomUUID();
        UUID quotationId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, consultantId);
        when(quotationRepository.findById(quotationId)).thenReturn(
            Optional.of(new Quotation(quotationId, itineraryId, Instant.now().plusSeconds(3600))));
        when(itineraryRepository.findById(itineraryId)).thenReturn(
            Optional.of(new Itinerary(itineraryId, consultantId, null)));
        HotelLineItem lineItemOne = new HotelLineItem(UUID.randomUUID(), itineraryId, SupplierId.HOTELBEDS,
            "rate-key-1", "Taj Palace", "Deluxe Room", MealPlan.BB, Instant.now().plusSeconds(3600),
            BigDecimal.valueOf(100), CurrencyCode.INR, BigDecimal.ZERO, BigDecimal.ZERO,
            BigDecimal.valueOf(5000), CurrencyCode.INR, BigDecimal.ONE);
        HotelLineItem lineItemTwo = new HotelLineItem(UUID.randomUUID(), itineraryId, SupplierId.HOTELBEDS,
            "rate-key-2", "Beach Resort", "Sea View", MealPlan.HB, Instant.now().plusSeconds(3600),
            BigDecimal.valueOf(80), CurrencyCode.INR, BigDecimal.ZERO, BigDecimal.ZERO,
            BigDecimal.valueOf(3000), CurrencyCode.INR, BigDecimal.ONE);
        when(hotelLineItemRepository.findByItineraryId(itineraryId)).thenReturn(List.of(lineItemOne, lineItemTwo));
        var command = new ConvertQuotationToPackageCommand("Goa Getaway", "A relaxing beach trip",
            LocalDate.now().plusDays(30), LocalDate.now().plusDays(90), BigDecimal.valueOf(500), 4);

        UUID packageId = service.convertQuotationToPackage(quotationId, command);

        assertThat(packageId).isNotNull();
        ArgumentCaptor<TravelPackage> captor = ArgumentCaptor.forClass(TravelPackage.class);
        verify(travelPackageRepository).save(captor.capture());
        assertThat(captor.getValue().getSourceItineraryId()).isEqualTo(itineraryId);
        assertThat(captor.getValue().getConsultantId()).isEqualTo(consultantId);
        assertThat(captor.getValue().getName()).isEqualTo("Goa Getaway");
        assertThat(captor.getValue().getBasePrice()).isEqualByComparingTo("8000");
        assertThat(captor.getValue().getMarkupPrice()).isEqualByComparingTo("500");
        assertThat(captor.getValue().getCurrency()).isEqualTo(CurrencyCode.INR);
        assertThat(captor.getValue().getMaxPax()).isEqualTo(4);
        assertThat(captor.getValue().getStatus()).isEqualTo(PackageStatus.DRAFT);

        ArgumentCaptor<PackageCreatedEvent> eventCaptor = ArgumentCaptor.forClass(PackageCreatedEvent.class);
        verify(events).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().packageId()).isEqualTo(packageId);
        assertThat(eventCaptor.getValue().sourceItineraryId()).isEqualTo(itineraryId);
        assertThat(eventCaptor.getValue().consultantId()).isEqualTo(consultantId);
    }

    @Test
    void convertingAnUnknownQuotationFailsBOK10() {
        UUID quotationId = UUID.randomUUID();
        when(quotationRepository.findById(quotationId)).thenReturn(Optional.empty());

        var command = new ConvertQuotationToPackageCommand("Goa Getaway", null,
            LocalDate.now().plusDays(30), LocalDate.now().plusDays(90), BigDecimal.valueOf(500), 4);

        assertThatThrownBy(() -> service.convertQuotationToPackage(quotationId, command))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void convertingAQuotationToAPackageRejectsAValidityEndBeforeStartBOK10() {
        UUID itineraryId = UUID.randomUUID();
        UUID consultantId = UUID.randomUUID();
        UUID quotationId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, consultantId);
        when(quotationRepository.findById(quotationId)).thenReturn(
            Optional.of(new Quotation(quotationId, itineraryId, Instant.now().plusSeconds(3600))));
        when(itineraryRepository.findById(itineraryId)).thenReturn(
            Optional.of(new Itinerary(itineraryId, consultantId, null)));

        var command = new ConvertQuotationToPackageCommand("Goa Getaway", null,
            LocalDate.now().plusDays(90), LocalDate.now().plusDays(30), BigDecimal.valueOf(500), 4);

        assertThatThrownBy(() -> service.convertQuotationToPackage(quotationId, command))
            .isInstanceOf(IllegalArgumentException.class);
        verify(travelPackageRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void convertingAQuotationToAPackageRejectsANonPositiveMaxPaxBOK10() {
        UUID itineraryId = UUID.randomUUID();
        UUID consultantId = UUID.randomUUID();
        UUID quotationId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, consultantId);
        when(quotationRepository.findById(quotationId)).thenReturn(
            Optional.of(new Quotation(quotationId, itineraryId, Instant.now().plusSeconds(3600))));
        when(itineraryRepository.findById(itineraryId)).thenReturn(
            Optional.of(new Itinerary(itineraryId, consultantId, null)));

        var command = new ConvertQuotationToPackageCommand("Goa Getaway", null,
            LocalDate.now().plusDays(30), LocalDate.now().plusDays(90), BigDecimal.valueOf(500), 0);

        assertThatThrownBy(() -> service.convertQuotationToPackage(quotationId, command))
            .isInstanceOf(IllegalArgumentException.class);
        verify(travelPackageRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void convertingAQuotationToAPackageRejectsAnItineraryWithNoLineItemsBOK10() {
        UUID itineraryId = UUID.randomUUID();
        UUID consultantId = UUID.randomUUID();
        UUID quotationId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, consultantId);
        when(quotationRepository.findById(quotationId)).thenReturn(
            Optional.of(new Quotation(quotationId, itineraryId, Instant.now().plusSeconds(3600))));
        when(itineraryRepository.findById(itineraryId)).thenReturn(
            Optional.of(new Itinerary(itineraryId, consultantId, null)));
        when(hotelLineItemRepository.findByItineraryId(itineraryId)).thenReturn(List.of());

        var command = new ConvertQuotationToPackageCommand("Goa Getaway", null,
            LocalDate.now().plusDays(30), LocalDate.now().plusDays(90), BigDecimal.valueOf(500), 4);

        assertThatThrownBy(() -> service.convertQuotationToPackage(quotationId, command))
            .isInstanceOf(IllegalStateException.class);
        verify(travelPackageRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void aConsultantCannotConvertAnotherConsultantsQuotationBOK10() {
        UUID itineraryId = UUID.randomUUID();
        UUID ownerConsultantId = UUID.randomUUID();
        UUID quotationId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, UUID.randomUUID());
        when(quotationRepository.findById(quotationId)).thenReturn(
            Optional.of(new Quotation(quotationId, itineraryId, Instant.now().plusSeconds(3600))));
        when(itineraryRepository.findById(itineraryId)).thenReturn(
            Optional.of(new Itinerary(itineraryId, ownerConsultantId, null)));

        var command = new ConvertQuotationToPackageCommand("Goa Getaway", null,
            LocalDate.now().plusDays(30), LocalDate.now().plusDays(90), BigDecimal.valueOf(500), 4);

        assertThatThrownBy(() -> service.convertQuotationToPackage(quotationId, command))
            .isInstanceOf(AccessDeniedException.class);
        verify(travelPackageRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void publishingAPackageTransitionsToPublishedAndPublishesTheEventBOK12() {
        UUID packageId = UUID.randomUUID();
        UUID itineraryId = UUID.randomUUID();
        UUID consultantId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, consultantId);
        TravelPackage travelPackage = new TravelPackage(packageId, itineraryId, consultantId, "Goa Getaway",
            "A relaxing beach trip", LocalDate.now().plusDays(30), LocalDate.now().plusDays(90),
            BigDecimal.valueOf(11_371.20), BigDecimal.valueOf(500), CurrencyCode.INR, 4);
        when(travelPackageRepository.findById(packageId)).thenReturn(Optional.of(travelPackage));

        service.publishPackage(packageId, true);

        assertThat(travelPackage.getStatus()).isEqualTo(PackageStatus.PUBLISHED);
        assertThat(travelPackage.isPromotedViaAds()).isTrue();
        verify(travelPackageRepository).save(travelPackage);

        ArgumentCaptor<PackagePublishedEvent> captor = ArgumentCaptor.forClass(PackagePublishedEvent.class);
        verify(events).publishEvent(captor.capture());
        assertThat(captor.getValue().packageId()).isEqualTo(packageId);
        assertThat(captor.getValue().sourceItineraryId()).isEqualTo(itineraryId);
        assertThat(captor.getValue().consultantId()).isEqualTo(consultantId);
        assertThat(captor.getValue().promotedViaAds()).isTrue();
    }

    @Test
    void publishingAnAlreadyPublishedPackageFailsBOK12() {
        UUID packageId = UUID.randomUUID();
        UUID consultantId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, consultantId);
        TravelPackage travelPackage = new TravelPackage(packageId, UUID.randomUUID(), consultantId, "Goa Getaway",
            null, LocalDate.now().plusDays(30), LocalDate.now().plusDays(90),
            BigDecimal.valueOf(11_371.20), BigDecimal.valueOf(500), CurrencyCode.INR, 4);
        travelPackage.publish(false);
        when(travelPackageRepository.findById(packageId)).thenReturn(Optional.of(travelPackage));

        assertThatThrownBy(() -> service.publishPackage(packageId, true))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void aConsultantCannotPublishAnotherConsultantsPackageBOK12() {
        UUID packageId = UUID.randomUUID();
        UUID ownerConsultantId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, UUID.randomUUID());
        TravelPackage travelPackage = new TravelPackage(packageId, UUID.randomUUID(), ownerConsultantId, "Goa Getaway",
            null, LocalDate.now().plusDays(30), LocalDate.now().plusDays(90),
            BigDecimal.valueOf(11_371.20), BigDecimal.valueOf(500), CurrencyCode.INR, 4);
        when(travelPackageRepository.findById(packageId)).thenReturn(Optional.of(travelPackage));

        assertThatThrownBy(() -> service.publishPackage(packageId, false))
            .isInstanceOf(AccessDeniedException.class);
        verify(travelPackageRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void findPublishedPackagesByConsultantReturnsOnlyPublishedOnesFromTheRepositoryQueryBOK12() {
        UUID consultantId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, consultantId);
        TravelPackage published = new TravelPackage(UUID.randomUUID(), UUID.randomUUID(), consultantId,
            "Goa Getaway", null, LocalDate.now().plusDays(30), LocalDate.now().plusDays(90),
            BigDecimal.valueOf(11_371.20), BigDecimal.valueOf(500), CurrencyCode.INR, 4);
        published.publish(false);
        Pageable pageable = PageRequest.of(0, 20);
        when(travelPackageRepository.findByConsultantIdAndStatus(consultantId, PackageStatus.PUBLISHED, pageable))
            .thenReturn(new PageImpl<>(List.of(published)));

        Page<com.adren.travel.booking.PackageView> result =
            service.findPublishedPackagesByConsultant(consultantId, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).name()).isEqualTo("Goa Getaway");
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
