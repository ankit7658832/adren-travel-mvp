package com.adren.travel.booking.internal;

import com.adren.travel.booking.AddActivityLineItemCommand;
import com.adren.travel.booking.AddCruiseLineItemCommand;
import com.adren.travel.booking.AddFlightLineItemCommand;
import com.adren.travel.booking.AddHotelLineItemCommand;
import com.adren.travel.booking.AddTransferLineItemCommand;
import com.adren.travel.booking.AlternateOption;
import com.adren.travel.booking.CabinClass;
import com.adren.travel.booking.CalculateCancellationRefundCommand;
import com.adren.travel.booking.CancellationRequestView;
import com.adren.travel.booking.ConsolidateCheckoutTotalCommand;
import com.adren.travel.booking.ConvertQuotationToPackageCommand;
import com.adren.travel.booking.CreateTravelerProfileCommand;
import com.adren.travel.booking.DisputeTicketView;
import com.adren.travel.booking.FlagDisputeCommand;
import com.adren.travel.booking.GenerateAiSuggestionCommand;
import com.adren.travel.booking.MealPlan;
import com.adren.travel.booking.event.ActivityLineItemAddedEvent;
import com.adren.travel.booking.event.BookingCancelledEvent;
import com.adren.travel.booking.event.BookingConfirmedEvent;
import com.adren.travel.booking.event.CruiseLineItemAddedEvent;
import com.adren.travel.booking.event.DisputeTicketCreatedEvent;
import com.adren.travel.booking.event.FlightLineItemAddedEvent;
import com.adren.travel.booking.event.HotelLineItemAddedEvent;
import com.adren.travel.booking.event.ItineraryQuotationSavedEvent;
import com.adren.travel.booking.event.TransferLineItemAddedEvent;
import com.adren.travel.booking.event.PackageCreatedEvent;
import com.adren.travel.booking.event.PackagePublishedEvent;
import com.adren.travel.booking.event.QuotationRecalculatedEvent;
import com.adren.travel.booking.event.TravelerProfileCreatedEvent;
import com.adren.travel.payments.CalculateSellRateCommand;
import com.adren.travel.payments.FxRateSnapshot;
import com.adren.travel.payments.PaymentsApi;
import com.adren.travel.payments.RefundCalculation;
import com.adren.travel.payments.SellRateCalculation;
import com.adren.travel.payments.WalletHoldCommand;
import com.adren.travel.security.AdrenPrincipal;
import com.adren.travel.security.Role;
import com.adren.travel.shared.CurrencyCode;
import com.adren.travel.shared.Money;
import com.adren.travel.shared.ProductCategory;
import com.adren.travel.supplier.SupplierId;
import com.adren.travel.supplier.SupplierSearchApi;
import com.adren.travel.supplier.SupplierSearchResult;
import com.adren.travel.whitelabel.Market;
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
    FlightLineItemRepository flightLineItemRepository;

    @Mock
    TransferLineItemRepository transferLineItemRepository;

    @Mock
    CruiseLineItemRepository cruiseLineItemRepository;

    @Mock
    ActivityLineItemRepository activityLineItemRepository;

    @Mock
    BookingRepository bookingRepository;

    HotelDedupService hotelDedupService = new HotelDedupService();

    @Mock
    QuotationRepository quotationRepository;

    @Mock
    TravelPackageRepository travelPackageRepository;

    @Mock
    VoucherService voucherService;

    @Mock
    PaymentsApi paymentsApi;

    @Mock
    CancellationRequestRepository cancellationRequestRepository;

    @Mock
    DisputeTicketRepository disputeTicketRepository;

    @Mock
    com.adren.travel.ai.AiApi aiApi;

    BookingServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new BookingServiceImpl(itineraryRepository, travelerProfileRepository, hotelLineItemRepository,
            flightLineItemRepository, transferLineItemRepository, cruiseLineItemRepository,
            activityLineItemRepository, quotationRepository, travelPackageRepository, bookingRepository,
            voucherService, events, whitelabelApi, supplierSearchApi, hotelDedupService, paymentsApi,
            cancellationRequestRepository, disputeTicketRepository, aiApi);
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
    void generatingAnAiSuggestionMarksTheItineraryAiGeneratedFIN02() {
        UUID itineraryId = UUID.randomUUID();
        UUID consultantId = UUID.randomUUID();
        Itinerary draft = new Itinerary(itineraryId, consultantId, null);
        when(itineraryRepository.findById(itineraryId)).thenReturn(Optional.of(draft));
        authenticateAs(Role.CONSULTANT, consultantId);
        UUID auditLogId = UUID.randomUUID();
        com.adren.travel.ai.AiItinerarySuggestion suggestion = new com.adren.travel.ai.AiItinerarySuggestion(
            auditLogId, List.of());
        when(aiApi.generateItinerary(any())).thenReturn(suggestion);

        var result = service.generateAiItinerarySuggestion(itineraryId, new GenerateAiSuggestionCommand(
            "GOA", LocalDate.now().plusDays(30), LocalDate.now().plusDays(34), "A relaxing trip", null));

        assertThat(result).isSameAs(suggestion);
        assertThat(draft.isAiGenerated()).isTrue();
        assertThat(draft.getAiAuditLogId()).isEqualTo(auditLogId);
        assertThat(draft.isAiApproved()).isFalse();
        verify(itineraryRepository).save(draft);
    }

    @Test
    void generatingANoViableSuggestionNeverMarksTheItineraryAiGeneratedFIN05() {
        UUID itineraryId = UUID.randomUUID();
        UUID consultantId = UUID.randomUUID();
        Itinerary draft = new Itinerary(itineraryId, consultantId, null);
        when(itineraryRepository.findById(itineraryId)).thenReturn(Optional.of(draft));
        authenticateAs(Role.CONSULTANT, consultantId);
        when(aiApi.generateItinerary(any()))
            .thenReturn(new com.adren.travel.ai.NoViableSuggestion(UUID.randomUUID(), "No inventory available"));

        service.generateAiItinerarySuggestion(itineraryId, new GenerateAiSuggestionCommand(
            "GOA", LocalDate.now().plusDays(30), LocalDate.now().plusDays(34), "A relaxing trip", null));

        assertThat(draft.isAiGenerated()).isFalse();
        verify(itineraryRepository, org.mockito.Mockito.never()).save(draft);
    }

    @Test
    void savingAsQuotationBlocksAnUnapprovedAiGeneratedItineraryFIN06() {
        UUID itineraryId = UUID.randomUUID();
        UUID consultantId = UUID.randomUUID();
        Itinerary draft = new Itinerary(itineraryId, consultantId, null);
        draft.markAiGenerated(UUID.randomUUID());
        when(itineraryRepository.findById(itineraryId)).thenReturn(Optional.of(draft));
        stubExistingHotelLineItem(itineraryId);
        authenticateAs(Role.CONSULTANT, consultantId);

        assertThatThrownBy(() -> service.saveAsQuotation(itineraryId))
            .isInstanceOf(com.adren.travel.booking.AiApprovalRequiredException.class);
        assertThat(draft.getStatus()).isEqualTo(ItineraryStatus.DRAFT);
    }

    @Test
    void savingAsQuotationSucceedsOnceTheAiSuggestionIsApprovedFIN06() {
        UUID itineraryId = UUID.randomUUID();
        UUID consultantId = UUID.randomUUID();
        Itinerary draft = new Itinerary(itineraryId, consultantId, null);
        draft.markAiGenerated(UUID.randomUUID());
        draft.markAiApproved();
        when(itineraryRepository.findById(itineraryId)).thenReturn(Optional.of(draft));
        stubExistingHotelLineItem(itineraryId);
        authenticateAs(Role.CONSULTANT, consultantId);

        service.saveAsQuotation(itineraryId);

        assertThat(draft.getStatus()).isEqualTo(ItineraryStatus.QUOTATION);
    }

    @Test
    void approveAiSuggestionMarksTheItineraryApprovedFIN06() {
        UUID itineraryId = UUID.randomUUID();
        UUID consultantId = UUID.randomUUID();
        Itinerary draft = new Itinerary(itineraryId, consultantId, null);
        draft.markAiGenerated(UUID.randomUUID());
        when(itineraryRepository.findById(itineraryId)).thenReturn(Optional.of(draft));
        authenticateAs(Role.CONSULTANT, consultantId);

        service.approveAiSuggestion(itineraryId);

        assertThat(draft.isAiApproved()).isTrue();
        verify(itineraryRepository).save(draft);
    }

    @Test
    void approvingWithNoAiSuggestionEverGeneratedThrowsFIN06() {
        UUID itineraryId = UUID.randomUUID();
        UUID consultantId = UUID.randomUUID();
        Itinerary draft = new Itinerary(itineraryId, consultantId, null);
        when(itineraryRepository.findById(itineraryId)).thenReturn(Optional.of(draft));
        authenticateAs(Role.CONSULTANT, consultantId);

        assertThatThrownBy(() -> service.approveAiSuggestion(itineraryId))
            .isInstanceOf(IllegalStateException.class);
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
        UUID sourceItineraryId = UUID.randomUUID();
        when(quotationRepository.findById(packageId)).thenReturn(Optional.empty());
        TravelPackage travelPackage = new TravelPackage(packageId, sourceItineraryId, consultantId, "Goa Getaway",
            null, LocalDate.now().plusDays(30), LocalDate.now().plusDays(90),
            BigDecimal.valueOf(11_371.20), BigDecimal.valueOf(500), CurrencyCode.INR, 4);
        when(travelPackageRepository.findById(packageId)).thenReturn(Optional.of(travelPackage));
        // BOK-16: lockForBooking resolves the package's source itinerary too.
        Itinerary sourceItinerary = new Itinerary(sourceItineraryId, consultantId, null);
        sourceItinerary.markAsQuotation();
        when(itineraryRepository.findById(sourceItineraryId)).thenReturn(Optional.of(sourceItinerary));
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
        // BOK-16: confirmBooking's lockForBooking calls markAsBooked(),
        // which requires QUOTATION (not the constructor's default DRAFT) —
        // a quotation's source itinerary is always past DRAFT by definition.
        Itinerary itinerary = new Itinerary(itineraryId, consultantId, null);
        itinerary.markAsQuotation();
        when(itineraryRepository.findById(itineraryId)).thenReturn(Optional.of(itinerary));
        return quotationId;
    }

    @Test
    void confirmBookingGeneratesAVoucherInTheSameCallAsTheBookingConfirmedEventBOK15() {
        UUID consultantId = UUID.randomUUID();
        UUID quotationId = stubQuotationResolvingTo(consultantId);
        Money price = new Money(BigDecimal.valueOf(11_500), CurrencyCode.INR);
        authenticateAs(Role.CONSULTANT, consultantId);

        UUID bookingId = service.confirmBooking(quotationId, price);

        verify(voucherService).generateFor(bookingId);
    }

    @Test
    void confirmBookingFromPaymentWebhookAlsoGeneratesAVoucherBOK15() {
        UUID consultantId = UUID.randomUUID();
        Money price = new Money(BigDecimal.valueOf(11_500), CurrencyCode.INR);

        UUID bookingId = service.confirmBookingFromPaymentWebhook(UUID.randomUUID(), consultantId, price);

        verify(voucherService).generateFor(bookingId);
    }

    @Test
    void confirmBookingMapsALostOptimisticLockRaceToInventoryNoLongerAvailableBOK16() {
        UUID consultantId = UUID.randomUUID();
        UUID quotationId = stubQuotationResolvingTo(consultantId);
        Money price = new Money(BigDecimal.valueOf(11_500), CurrencyCode.INR);
        authenticateAs(Role.CONSULTANT, consultantId);
        when(itineraryRepository.saveAndFlush(any()))
            .thenThrow(new org.springframework.orm.ObjectOptimisticLockingFailureException(Itinerary.class, quotationId));

        assertThatThrownBy(() -> service.confirmBooking(quotationId, price))
            .isInstanceOf(com.adren.travel.booking.InventoryNoLongerAvailableException.class);
        verify(paymentsApi, org.mockito.Mockito.never()).placeHold(any());
        verify(events, org.mockito.Mockito.never()).publishEvent(any(BookingConfirmedEvent.class));
    }

    @Test
    void confirmBookingPersistsABookingRowWithAPnrSearchableRefBOK19() {
        UUID consultantId = UUID.randomUUID();
        UUID quotationId = stubQuotationResolvingTo(consultantId);
        Money price = new Money(BigDecimal.valueOf(11_500), CurrencyCode.INR);
        authenticateAs(Role.CONSULTANT, consultantId);

        UUID bookingId = service.confirmBooking(quotationId, price);

        ArgumentCaptor<Booking> captor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(captor.capture());
        assertThat(captor.getValue().getBookingId()).isEqualTo(bookingId);
        assertThat(captor.getValue().getConsultantId()).isEqualTo(consultantId);
        assertThat(captor.getValue().getPaymentMethod()).isEqualTo(PaymentMethod.WALLET);
        assertThat(captor.getValue().getPnrSearchableRef()).isNotBlank();
        assertThat(captor.getValue().getStatus()).isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    void confirmBookingFromPaymentWebhookPersistsAStripePaymentMethodBookingBOK19() {
        UUID consultantId = UUID.randomUUID();
        Money price = new Money(BigDecimal.valueOf(11_500), CurrencyCode.INR);

        service.confirmBookingFromPaymentWebhook(UUID.randomUUID(), consultantId, price);

        ArgumentCaptor<Booking> captor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(captor.capture());
        assertThat(captor.getValue().getPaymentMethod()).isEqualTo(PaymentMethod.STRIPE);
        assertThat(captor.getValue().getItineraryId()).isNull();
    }

    @Test
    void confirmBookingRetriesPnrGenerationOnACollisionBOK19() {
        UUID consultantId = UUID.randomUUID();
        UUID quotationId = stubQuotationResolvingTo(consultantId);
        Money price = new Money(BigDecimal.valueOf(11_500), CurrencyCode.INR);
        authenticateAs(Role.CONSULTANT, consultantId);
        // First-generated PNR "collides"; retry must produce a save with a
        // still-unique ref rather than giving up or overwriting silently.
        when(bookingRepository.existsByPnrSearchableRef(any())).thenReturn(true, false);

        service.confirmBooking(quotationId, price);

        verify(bookingRepository, org.mockito.Mockito.times(2)).existsByPnrSearchableRef(any());
        verify(bookingRepository).save(any());
    }

    @Test
    void confirmBookingOnAccountNeverTouchesTheWalletHoldMachineryFIN12() {
        UUID consultantId = UUID.randomUUID();
        UUID quotationId = stubQuotationResolvingTo(consultantId);
        Money price = new Money(BigDecimal.valueOf(11_500), CurrencyCode.INR);
        authenticateAs(Role.CONSULTANT, consultantId);

        UUID bookingId = service.confirmBookingOnAccount(quotationId, price);

        assertThat(bookingId).isNotNull();
        verify(paymentsApi).payOnAccount(new com.adren.travel.payments.WalletHoldCommand(bookingId, consultantId, price));
        verify(paymentsApi, org.mockito.Mockito.never()).placeHold(any());
        verify(paymentsApi, org.mockito.Mockito.never()).resolveHoldAsDebit(any());
        verify(events).publishEvent(any(BookingConfirmedEvent.class));
    }

    @Test
    void confirmBookingOnAccountFailsForAnUnknownQuotationOrPackageBOK13() {
        UUID unknownId = UUID.randomUUID();
        Money price = new Money(BigDecimal.valueOf(1000), CurrencyCode.INR);
        when(quotationRepository.findById(unknownId)).thenReturn(Optional.empty());
        when(travelPackageRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.confirmBookingOnAccount(unknownId, price))
            .isInstanceOf(IllegalArgumentException.class);
        org.mockito.Mockito.verifyNoInteractions(paymentsApi);
    }

    @Test
    void calculateCancellationRefundDelegatesToPaymentsApiWithTheBookingsConsultantIdFIN13() {
        UUID bookingId = UUID.randomUUID();
        UUID consultantId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, consultantId);
        Booking booking = new Booking(bookingId, UUID.randomUUID(), consultantId, BigDecimal.valueOf(10_000),
            CurrencyCode.INR, PaymentMethod.WALLET, "ABCD1234");
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        Money sellPrice = new Money(BigDecimal.valueOf(10_000), CurrencyCode.INR);
        Instant deadline = Instant.now().plusSeconds(3600);
        Instant cancelledAt = Instant.now();
        com.adren.travel.payments.FxRateSnapshot originalFxRateSnapshot = new com.adren.travel.payments.FxRateSnapshot(
            CurrencyCode.USD, CurrencyCode.INR, BigDecimal.valueOf(80), Instant.now().minusSeconds(7200));
        com.adren.travel.payments.RefundCalculation expected = new com.adren.travel.payments.RefundCalculation(
            sellPrice, Money.zero(CurrencyCode.INR), false, new Money(BigDecimal.valueOf(125), CurrencyCode.USD));
        when(paymentsApi.calculateRefund(any())).thenReturn(expected);

        com.adren.travel.payments.RefundCalculation result = service.calculateCancellationRefund(bookingId,
            new com.adren.travel.booking.CalculateCancellationRefundCommand(
                sellPrice, deadline, cancelledAt, BigDecimal.valueOf(30), originalFxRateSnapshot));

        assertThat(result).isEqualTo(expected);
        ArgumentCaptor<com.adren.travel.payments.CalculateRefundCommand> captor =
            ArgumentCaptor.forClass(com.adren.travel.payments.CalculateRefundCommand.class);
        verify(paymentsApi).calculateRefund(captor.capture());
        assertThat(captor.getValue().bookingId()).isEqualTo(bookingId);
        assertThat(captor.getValue().consultantId()).isEqualTo(consultantId);
    }

    @Test
    void calculateCancellationRefundFailsForAnUnknownBookingFIN13() {
        UUID bookingId = UUID.randomUUID();
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.empty());
        com.adren.travel.payments.FxRateSnapshot originalFxRateSnapshot = new com.adren.travel.payments.FxRateSnapshot(
            CurrencyCode.USD, CurrencyCode.INR, BigDecimal.valueOf(80), Instant.now().minusSeconds(7200));

        assertThatThrownBy(() -> service.calculateCancellationRefund(bookingId,
            new com.adren.travel.booking.CalculateCancellationRefundCommand(
                new Money(BigDecimal.valueOf(1000), CurrencyCode.INR), Instant.now(), Instant.now(), BigDecimal.ZERO,
                originalFxRateSnapshot)))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void submitCancellationProcessesTheRefundImmediatelyWhenNoPenaltyAppliesFIN16() {
        UUID bookingId = UUID.randomUUID();
        UUID consultantId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, consultantId);
        Booking booking = new Booking(bookingId, UUID.randomUUID(), consultantId, BigDecimal.valueOf(10_000),
            CurrencyCode.INR, PaymentMethod.WALLET, "ABCD1234");
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        Money sellPrice = new Money(BigDecimal.valueOf(10_000), CurrencyCode.INR);
        FxRateSnapshot originalFxRateSnapshot = new FxRateSnapshot(CurrencyCode.USD, CurrencyCode.INR,
            BigDecimal.valueOf(80), Instant.now().minusSeconds(7200));
        RefundCalculation calculation = new RefundCalculation(sellPrice, Money.zero(CurrencyCode.INR), false,
            new Money(BigDecimal.valueOf(125), CurrencyCode.USD));
        when(paymentsApi.calculateRefund(any())).thenReturn(calculation);

        CancellationRequestView view = service.submitCancellation(bookingId,
            new CalculateCancellationRefundCommand(sellPrice, Instant.now().plusSeconds(3600), Instant.now(),
                BigDecimal.valueOf(30), originalFxRateSnapshot));

        assertThat(view.status()).isEqualTo("REFUNDED");
        verify(paymentsApi).processRefund(new WalletHoldCommand(bookingId, consultantId, sellPrice));
        verify(bookingRepository).save(booking);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);

        ArgumentCaptor<BookingCancelledEvent> captor = ArgumentCaptor.forClass(BookingCancelledEvent.class);
        verify(events).publishEvent(captor.capture());
        assertThat(captor.getValue().bookingId()).isEqualTo(bookingId);
    }

    @Test
    void submitCancellationPausesForApprovalWhenAPenaltyAppliesFIN16() {
        UUID bookingId = UUID.randomUUID();
        UUID consultantId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, consultantId);
        Booking booking = new Booking(bookingId, UUID.randomUUID(), consultantId, BigDecimal.valueOf(10_000),
            CurrencyCode.INR, PaymentMethod.WALLET, "ABCD1234");
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        Money sellPrice = new Money(BigDecimal.valueOf(10_000), CurrencyCode.INR);
        FxRateSnapshot originalFxRateSnapshot = new FxRateSnapshot(CurrencyCode.USD, CurrencyCode.INR,
            BigDecimal.valueOf(80), Instant.now().minusSeconds(7200));
        RefundCalculation calculation = new RefundCalculation(new Money(BigDecimal.valueOf(7000), CurrencyCode.INR),
            new Money(BigDecimal.valueOf(3000), CurrencyCode.INR), true,
            new Money(BigDecimal.valueOf(87.50), CurrencyCode.USD));
        when(paymentsApi.calculateRefund(any())).thenReturn(calculation);

        CancellationRequestView view = service.submitCancellation(bookingId,
            new CalculateCancellationRefundCommand(sellPrice, Instant.now().minusSeconds(3600), Instant.now(),
                BigDecimal.valueOf(30), originalFxRateSnapshot));

        assertThat(view.status()).isEqualTo("PENDING_APPROVAL");
        verify(paymentsApi, org.mockito.Mockito.never()).processRefund(any());
        verify(bookingRepository, org.mockito.Mockito.never()).save(any());
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        verify(events, org.mockito.Mockito.never()).publishEvent(any(BookingCancelledEvent.class));
        verify(cancellationRequestRepository).save(any());
    }

    @Test
    void approveCancellationProcessesTheRefundAndCancelsTheBookingFIN16() {
        UUID bookingId = UUID.randomUUID();
        UUID consultantId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, consultantId);
        UUID cancellationRequestId = UUID.randomUUID();
        CancellationRequest request = CancellationRequest.submit(cancellationRequestId, bookingId, consultantId,
            BigDecimal.valueOf(7000), CurrencyCode.INR, BigDecimal.valueOf(3000), CurrencyCode.INR, true);
        when(cancellationRequestRepository.findById(cancellationRequestId)).thenReturn(Optional.of(request));
        Booking booking = new Booking(bookingId, UUID.randomUUID(), consultantId, BigDecimal.valueOf(10_000),
            CurrencyCode.INR, PaymentMethod.WALLET, "ABCD1234");
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        CancellationRequestView view = service.approveCancellation(cancellationRequestId);

        assertThat(view.status()).isEqualTo("REFUNDED");
        verify(paymentsApi).processRefund(new WalletHoldCommand(bookingId, consultantId,
            new Money(BigDecimal.valueOf(7000), CurrencyCode.INR)));
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        verify(events).publishEvent(any(BookingCancelledEvent.class));
    }

    @Test
    void approveCancellationFailsWhenNotPendingApprovalFIN16() {
        UUID bookingId = UUID.randomUUID();
        UUID consultantId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, consultantId);
        UUID cancellationRequestId = UUID.randomUUID();
        // requiresApproval=false starts life already APPROVED, never PENDING_APPROVAL.
        CancellationRequest request = CancellationRequest.submit(cancellationRequestId, bookingId, consultantId,
            BigDecimal.valueOf(10_000), CurrencyCode.INR, BigDecimal.ZERO, CurrencyCode.INR, false);
        when(cancellationRequestRepository.findById(cancellationRequestId)).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> service.approveCancellation(cancellationRequestId))
            .isInstanceOf(IllegalStateException.class);
        verify(paymentsApi, org.mockito.Mockito.never()).processRefund(any());
    }

    @Test
    void flagDisputeCreatesATrackedTicketMarksTheBookingDisputedAndPublishesAnEventFIN16() {
        UUID bookingId = UUID.randomUUID();
        UUID consultantId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, consultantId);
        Booking booking = new Booking(bookingId, UUID.randomUUID(), consultantId, BigDecimal.valueOf(10_000),
            CurrencyCode.INR, PaymentMethod.WALLET, "ABCD1234");
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        DisputeTicketView view = service.flagDispute(bookingId, new FlagDisputeCommand("Wrong hotel room type"));

        assertThat(view.bookingId()).isEqualTo(bookingId);
        assertThat(view.status()).isEqualTo("OPEN");
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.DISPUTED);
        verify(disputeTicketRepository).save(any());

        ArgumentCaptor<DisputeTicketCreatedEvent> captor = ArgumentCaptor.forClass(DisputeTicketCreatedEvent.class);
        verify(events).publishEvent(captor.capture());
        assertThat(captor.getValue().bookingId()).isEqualTo(bookingId);
        assertThat(captor.getValue().reason()).isEqualTo("Wrong hotel room type");
    }

    @Test
    void flagDisputeFailsForAnUnknownBookingFIN16() {
        UUID bookingId = UUID.randomUUID();
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.flagDispute(bookingId, new FlagDisputeCommand("reason")))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void confirmBookingPlacesThenResolvesAWalletHoldForTheDirectPathFIN07() {
        UUID consultantId = UUID.randomUUID();
        UUID quotationId = stubQuotationResolvingTo(consultantId);
        Money price = new Money(BigDecimal.valueOf(11_500), CurrencyCode.INR);
        authenticateAs(Role.CONSULTANT, consultantId);

        UUID bookingId = service.confirmBooking(quotationId, price);

        var inOrder = org.mockito.Mockito.inOrder(paymentsApi);
        inOrder.verify(paymentsApi).placeHold(new com.adren.travel.payments.WalletHoldCommand(bookingId, consultantId, price));
        inOrder.verify(paymentsApi).resolveHoldAsDebit(new com.adren.travel.payments.WalletHoldCommand(bookingId, consultantId, price));
    }

    @Test
    void confirmBookingFromPaymentWebhookNeverTouchesTheWalletFIN07() {
        UUID consultantId = UUID.randomUUID();
        Money price = new Money(BigDecimal.valueOf(11_500), CurrencyCode.INR);

        service.confirmBookingFromPaymentWebhook(UUID.randomUUID(), consultantId, price);

        verify(paymentsApi, org.mockito.Mockito.never()).placeHold(any());
        verify(paymentsApi, org.mockito.Mockito.never()).resolveHoldAsDebit(any());
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
    void findAlternatesDeduplicatesTheSamePropertyAcrossSuppliersBOK20() {
        authenticateAs(Role.CONSULTANT, UUID.randomUUID());
        LocalDate checkIn = LocalDate.now().plusDays(30);
        LocalDate checkOut = checkIn.plusDays(3);
        when(supplierSearchApi.searchHotels("Goa", checkIn, checkOut)).thenReturn(List.of(
            new SupplierSearchResult(SupplierId.HOTELBEDS, "hb-1", "Taj Palace", "Deluxe",
                new Money(BigDecimal.valueOf(5000), CurrencyCode.INR), 4.0),
            new SupplierSearchResult(SupplierId.STUBA, "st-1", "Taj Palace", "Standard",
                new Money(BigDecimal.valueOf(4800), CurrencyCode.INR), 4.2)));

        List<AlternateOption> alternates =
            service.findAlternates(UUID.randomUUID(), "Goa", "hotel", checkIn, checkOut);

        assertThat(alternates).hasSize(1);
        assertThat(alternates.get(0).supplierId()).isEqualTo("STUBA"); // lower net rate wins the merge
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
    void addingAFlightLineItemPricesItThroughTheSellRatePipelineAndPublishesTheEventBOK04() {
        UUID itineraryId = UUID.randomUUID();
        UUID consultantId = UUID.randomUUID();
        Itinerary itinerary = new Itinerary(itineraryId, consultantId, null);
        when(itineraryRepository.findById(itineraryId)).thenReturn(Optional.of(itinerary));
        authenticateAs(Role.CONSULTANT, consultantId);

        Money netRate = new Money(BigDecimal.valueOf(5000), CurrencyCode.INR);
        Money fxConvertedBase = new Money(BigDecimal.valueOf(5000), CurrencyCode.INR);
        Money bufferedAmount = new Money(BigDecimal.valueOf(5150), CurrencyCode.INR);
        Money markupAmount = new Money(BigDecimal.valueOf(772.50), CurrencyCode.INR);
        Money sellRate = new Money(BigDecimal.valueOf(5922.50), CurrencyCode.INR);
        Money commissionAmount = Money.zero(CurrencyCode.INR);
        FxRateSnapshot snapshot = new FxRateSnapshot(CurrencyCode.INR, CurrencyCode.INR, BigDecimal.ONE, Instant.now());
        when(paymentsApi.calculateSellRate(any())).thenReturn(new SellRateCalculation(
            netRate, snapshot, fxConvertedBase, bufferedAmount, markupAmount, sellRate, commissionAmount));

        var command = new AddFlightLineItemCommand(SupplierId.MYSTIFLY, "fare-basis-1", "AI", "AI101",
            CabinClass.ECONOMY, "23kg", netRate, CurrencyCode.INR, BigDecimal.ONE, BigDecimal.valueOf(3), BigDecimal.ZERO);

        UUID lineItemId = service.addFlightLineItem(itineraryId, command);

        ArgumentCaptor<CalculateSellRateCommand> priceCaptor = ArgumentCaptor.forClass(CalculateSellRateCommand.class);
        verify(paymentsApi).calculateSellRate(priceCaptor.capture());
        assertThat(priceCaptor.getValue().category()).isEqualTo(ProductCategory.FLIGHT);
        assertThat(priceCaptor.getValue().consultantId()).isEqualTo(consultantId);

        ArgumentCaptor<FlightLineItem> lineItemCaptor = ArgumentCaptor.forClass(FlightLineItem.class);
        verify(flightLineItemRepository).save(lineItemCaptor.capture());
        assertThat(lineItemCaptor.getValue().getLineItemId()).isEqualTo(lineItemId);
        assertThat(lineItemCaptor.getValue().getAirlineCode()).isEqualTo("AI");
        assertThat(lineItemCaptor.getValue().getFlightNumber()).isEqualTo("AI101");
        assertThat(lineItemCaptor.getValue().getCabinClass()).isEqualTo(CabinClass.ECONOMY);
        assertThat(lineItemCaptor.getValue().getSellRate()).isEqualByComparingTo("5922.50");

        ArgumentCaptor<FlightLineItemAddedEvent> eventCaptor = ArgumentCaptor.forClass(FlightLineItemAddedEvent.class);
        verify(events).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().lineItemId()).isEqualTo(lineItemId);
        assertThat(eventCaptor.getValue().itineraryId()).isEqualTo(itineraryId);
        assertThat(eventCaptor.getValue().sellRate()).isEqualTo(sellRate);
    }

    @Test
    void addingAFlightLineItemFailsForAnItineraryOwnedByAnotherConsultantBOK04() {
        UUID itineraryId = UUID.randomUUID();
        UUID ownerConsultantId = UUID.randomUUID();
        Itinerary itinerary = new Itinerary(itineraryId, ownerConsultantId, null);
        when(itineraryRepository.findById(itineraryId)).thenReturn(Optional.of(itinerary));
        authenticateAs(Role.CONSULTANT, UUID.randomUUID());

        var command = new AddFlightLineItemCommand(SupplierId.MYSTIFLY, "fare-basis-1", "AI", "AI101",
            CabinClass.ECONOMY, "23kg", new Money(BigDecimal.valueOf(5000), CurrencyCode.INR), CurrencyCode.INR,
            BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO);

        assertThatThrownBy(() -> service.addFlightLineItem(itineraryId, command))
            .isInstanceOf(AccessDeniedException.class);
        verify(flightLineItemRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void addingAFlightLineItemRejectsAnItineraryThatIsAlreadyAQuotationBOK04() {
        UUID itineraryId = UUID.randomUUID();
        UUID consultantId = UUID.randomUUID();
        Itinerary itinerary = new Itinerary(itineraryId, consultantId, null);
        itinerary.markAsQuotation();
        when(itineraryRepository.findById(itineraryId)).thenReturn(Optional.of(itinerary));
        authenticateAs(Role.CONSULTANT, consultantId);

        var command = new AddFlightLineItemCommand(SupplierId.MYSTIFLY, "fare-basis-1", "AI", "AI101",
            CabinClass.ECONOMY, "23kg", new Money(BigDecimal.valueOf(5000), CurrencyCode.INR), CurrencyCode.INR,
            BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO);

        assertThatThrownBy(() -> service.addFlightLineItem(itineraryId, command))
            .isInstanceOf(IllegalStateException.class);
        verify(flightLineItemRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void addingATransferLineItemPricesItThroughTheSellRatePipelineAndPublishesTheEventBOK05() {
        UUID itineraryId = UUID.randomUUID();
        UUID consultantId = UUID.randomUUID();
        Itinerary itinerary = new Itinerary(itineraryId, consultantId, null);
        when(itineraryRepository.findById(itineraryId)).thenReturn(Optional.of(itinerary));
        authenticateAs(Role.CONSULTANT, consultantId);

        Money netRate = new Money(BigDecimal.valueOf(1200), CurrencyCode.INR);
        Money fxConvertedBase = new Money(BigDecimal.valueOf(1200), CurrencyCode.INR);
        Money bufferedAmount = new Money(BigDecimal.valueOf(1236), CurrencyCode.INR);
        Money markupAmount = new Money(BigDecimal.valueOf(185.40), CurrencyCode.INR);
        Money sellRate = new Money(BigDecimal.valueOf(1421.40), CurrencyCode.INR);
        Money commissionAmount = Money.zero(CurrencyCode.INR);
        FxRateSnapshot snapshot = new FxRateSnapshot(CurrencyCode.INR, CurrencyCode.INR, BigDecimal.ONE, Instant.now());
        when(paymentsApi.calculateSellRate(any())).thenReturn(new SellRateCalculation(
            netRate, snapshot, fxConvertedBase, bufferedAmount, markupAmount, sellRate, commissionAmount));

        var command = new AddTransferLineItemCommand(SupplierId.TRANSFERZ, "transfer-option-1", "Sedan",
            "BOM Airport", "Hotel Taj", netRate, CurrencyCode.INR, BigDecimal.ONE, BigDecimal.valueOf(3), BigDecimal.ZERO);

        UUID lineItemId = service.addTransferLineItem(itineraryId, command);

        ArgumentCaptor<CalculateSellRateCommand> priceCaptor = ArgumentCaptor.forClass(CalculateSellRateCommand.class);
        verify(paymentsApi).calculateSellRate(priceCaptor.capture());
        assertThat(priceCaptor.getValue().category()).isEqualTo(ProductCategory.TRANSFER);
        assertThat(priceCaptor.getValue().consultantId()).isEqualTo(consultantId);

        ArgumentCaptor<TransferLineItem> lineItemCaptor = ArgumentCaptor.forClass(TransferLineItem.class);
        verify(transferLineItemRepository).save(lineItemCaptor.capture());
        assertThat(lineItemCaptor.getValue().getLineItemId()).isEqualTo(lineItemId);
        assertThat(lineItemCaptor.getValue().getVehicleType()).isEqualTo("Sedan");
        assertThat(lineItemCaptor.getValue().getPickupPoint()).isEqualTo("BOM Airport");
        assertThat(lineItemCaptor.getValue().getDropoffPoint()).isEqualTo("Hotel Taj");
        assertThat(lineItemCaptor.getValue().getSellRate()).isEqualByComparingTo("1421.40");

        ArgumentCaptor<TransferLineItemAddedEvent> eventCaptor = ArgumentCaptor.forClass(TransferLineItemAddedEvent.class);
        verify(events).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().lineItemId()).isEqualTo(lineItemId);
        assertThat(eventCaptor.getValue().itineraryId()).isEqualTo(itineraryId);
        assertThat(eventCaptor.getValue().sellRate()).isEqualTo(sellRate);
    }

    @Test
    void addingATransferLineItemFailsForAnItineraryOwnedByAnotherConsultantBOK05() {
        UUID itineraryId = UUID.randomUUID();
        UUID ownerConsultantId = UUID.randomUUID();
        Itinerary itinerary = new Itinerary(itineraryId, ownerConsultantId, null);
        when(itineraryRepository.findById(itineraryId)).thenReturn(Optional.of(itinerary));
        authenticateAs(Role.CONSULTANT, UUID.randomUUID());

        var command = new AddTransferLineItemCommand(SupplierId.TRANSFERZ, "transfer-option-1", "Sedan",
            "BOM Airport", "Hotel Taj", new Money(BigDecimal.valueOf(1200), CurrencyCode.INR), CurrencyCode.INR,
            BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO);

        assertThatThrownBy(() -> service.addTransferLineItem(itineraryId, command))
            .isInstanceOf(AccessDeniedException.class);
        verify(transferLineItemRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void addingATransferLineItemRejectsAnItineraryThatIsAlreadyAQuotationBOK05() {
        UUID itineraryId = UUID.randomUUID();
        UUID consultantId = UUID.randomUUID();
        Itinerary itinerary = new Itinerary(itineraryId, consultantId, null);
        itinerary.markAsQuotation();
        when(itineraryRepository.findById(itineraryId)).thenReturn(Optional.of(itinerary));
        authenticateAs(Role.CONSULTANT, consultantId);

        var command = new AddTransferLineItemCommand(SupplierId.TRANSFERZ, "transfer-option-1", "Sedan",
            "BOM Airport", "Hotel Taj", new Money(BigDecimal.valueOf(1200), CurrencyCode.INR), CurrencyCode.INR,
            BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO);

        assertThatThrownBy(() -> service.addTransferLineItem(itineraryId, command))
            .isInstanceOf(IllegalStateException.class);
        verify(transferLineItemRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void addingACruiseLineItemPricesItThroughTheSellRatePipelineAndFlattensPortsAsMetadataBOK06() {
        UUID itineraryId = UUID.randomUUID();
        UUID consultantId = UUID.randomUUID();
        Itinerary itinerary = new Itinerary(itineraryId, consultantId, null);
        when(itineraryRepository.findById(itineraryId)).thenReturn(Optional.of(itinerary));
        authenticateAs(Role.CONSULTANT, consultantId);

        Money netRate = new Money(BigDecimal.valueOf(35000), CurrencyCode.INR);
        Money fxConvertedBase = new Money(BigDecimal.valueOf(35000), CurrencyCode.INR);
        Money bufferedAmount = new Money(BigDecimal.valueOf(36050), CurrencyCode.INR);
        Money markupAmount = new Money(BigDecimal.valueOf(5407.50), CurrencyCode.INR);
        Money sellRate = new Money(BigDecimal.valueOf(41457.50), CurrencyCode.INR);
        Money commissionAmount = Money.zero(CurrencyCode.INR);
        FxRateSnapshot snapshot = new FxRateSnapshot(CurrencyCode.INR, CurrencyCode.INR, BigDecimal.ONE, Instant.now());
        when(paymentsApi.calculateSellRate(any())).thenReturn(new SellRateCalculation(
            netRate, snapshot, fxConvertedBase, bufferedAmount, markupAmount, sellRate, commissionAmount));

        var command = new AddCruiseLineItemCommand(SupplierId.WIDGETY, "sailing-1", "Stub Cruise Line",
            "Balcony Cabin", List.of("Port A", "Port B"), true, netRate, CurrencyCode.INR, BigDecimal.ONE,
            BigDecimal.valueOf(3), BigDecimal.ZERO);

        UUID lineItemId = service.addCruiseLineItem(itineraryId, command);

        ArgumentCaptor<CalculateSellRateCommand> priceCaptor = ArgumentCaptor.forClass(CalculateSellRateCommand.class);
        verify(paymentsApi).calculateSellRate(priceCaptor.capture());
        assertThat(priceCaptor.getValue().category()).isEqualTo(ProductCategory.CRUISE);

        ArgumentCaptor<CruiseLineItem> lineItemCaptor = ArgumentCaptor.forClass(CruiseLineItem.class);
        verify(cruiseLineItemRepository).save(lineItemCaptor.capture());
        assertThat(lineItemCaptor.getValue().getLineItemId()).isEqualTo(lineItemId);
        assertThat(lineItemCaptor.getValue().getCruiseLine()).isEqualTo("Stub Cruise Line");
        // Ports are flattened as metadata on this single line item, not
        // separate line items (§10.2.6) — one saved entity, multiple ports.
        assertThat(lineItemCaptor.getValue().getPorts()).containsExactly("Port A", "Port B");
        assertThat(lineItemCaptor.getValue().isPassengerDocumentsRequired()).isTrue();
        assertThat(lineItemCaptor.getValue().getSellRate()).isEqualByComparingTo("41457.50");

        ArgumentCaptor<CruiseLineItemAddedEvent> eventCaptor = ArgumentCaptor.forClass(CruiseLineItemAddedEvent.class);
        verify(events).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().lineItemId()).isEqualTo(lineItemId);
    }

    @Test
    void addingACruiseLineItemFailsForAnItineraryOwnedByAnotherConsultantBOK06() {
        UUID itineraryId = UUID.randomUUID();
        UUID ownerConsultantId = UUID.randomUUID();
        Itinerary itinerary = new Itinerary(itineraryId, ownerConsultantId, null);
        when(itineraryRepository.findById(itineraryId)).thenReturn(Optional.of(itinerary));
        authenticateAs(Role.CONSULTANT, UUID.randomUUID());

        var command = new AddCruiseLineItemCommand(SupplierId.WIDGETY, "sailing-1", "Stub Cruise Line",
            "Balcony Cabin", List.of("Port A"), false, new Money(BigDecimal.valueOf(35000), CurrencyCode.INR),
            CurrencyCode.INR, BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO);

        assertThatThrownBy(() -> service.addCruiseLineItem(itineraryId, command))
            .isInstanceOf(AccessDeniedException.class);
        verify(cruiseLineItemRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void addingACruiseLineItemRejectsAnItineraryThatIsAlreadyAQuotationBOK06() {
        UUID itineraryId = UUID.randomUUID();
        UUID consultantId = UUID.randomUUID();
        Itinerary itinerary = new Itinerary(itineraryId, consultantId, null);
        itinerary.markAsQuotation();
        when(itineraryRepository.findById(itineraryId)).thenReturn(Optional.of(itinerary));
        authenticateAs(Role.CONSULTANT, consultantId);

        var command = new AddCruiseLineItemCommand(SupplierId.WIDGETY, "sailing-1", "Stub Cruise Line",
            "Balcony Cabin", List.of("Port A"), false, new Money(BigDecimal.valueOf(35000), CurrencyCode.INR),
            CurrencyCode.INR, BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO);

        assertThatThrownBy(() -> service.addCruiseLineItem(itineraryId, command))
            .isInstanceOf(IllegalStateException.class);
        verify(cruiseLineItemRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void addingAnActivityLineItemPricesItThroughTheSellRatePipelineAndPublishesTheEventBOK07() {
        UUID itineraryId = UUID.randomUUID();
        UUID consultantId = UUID.randomUUID();
        Itinerary itinerary = new Itinerary(itineraryId, consultantId, null);
        when(itineraryRepository.findById(itineraryId)).thenReturn(Optional.of(itinerary));
        authenticateAs(Role.CONSULTANT, consultantId);

        Money netRate = new Money(BigDecimal.valueOf(2000), CurrencyCode.INR);
        Money fxConvertedBase = new Money(BigDecimal.valueOf(2000), CurrencyCode.INR);
        Money bufferedAmount = new Money(BigDecimal.valueOf(2060), CurrencyCode.INR);
        Money markupAmount = new Money(BigDecimal.valueOf(309), CurrencyCode.INR);
        Money sellRate = new Money(BigDecimal.valueOf(2369), CurrencyCode.INR);
        Money commissionAmount = Money.zero(CurrencyCode.INR);
        FxRateSnapshot snapshot = new FxRateSnapshot(CurrencyCode.INR, CurrencyCode.INR, BigDecimal.ONE, Instant.now());
        when(paymentsApi.calculateSellRate(any())).thenReturn(new SellRateCalculation(
            netRate, snapshot, fxConvertedBase, bufferedAmount, markupAmount, sellRate, commissionAmount));

        var command = new AddActivityLineItemCommand(SupplierId.HBACTIVITIES, "activity-1", 120,
            java.time.LocalTime.of(9, 0), 4, netRate, CurrencyCode.INR, BigDecimal.ONE, BigDecimal.valueOf(3), BigDecimal.ZERO);

        UUID lineItemId = service.addActivityLineItem(itineraryId, command);

        ArgumentCaptor<CalculateSellRateCommand> priceCaptor = ArgumentCaptor.forClass(CalculateSellRateCommand.class);
        verify(paymentsApi).calculateSellRate(priceCaptor.capture());
        assertThat(priceCaptor.getValue().category()).isEqualTo(ProductCategory.ACTIVITY);

        ArgumentCaptor<ActivityLineItem> lineItemCaptor = ArgumentCaptor.forClass(ActivityLineItem.class);
        verify(activityLineItemRepository).save(lineItemCaptor.capture());
        assertThat(lineItemCaptor.getValue().getLineItemId()).isEqualTo(lineItemId);
        assertThat(lineItemCaptor.getValue().getDurationMinutes()).isEqualTo(120);
        assertThat(lineItemCaptor.getValue().getTimeSlot()).isEqualTo(java.time.LocalTime.of(9, 0));
        assertThat(lineItemCaptor.getValue().getHeadcount()).isEqualTo(4);

        ArgumentCaptor<ActivityLineItemAddedEvent> eventCaptor = ArgumentCaptor.forClass(ActivityLineItemAddedEvent.class);
        verify(events).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().lineItemId()).isEqualTo(lineItemId);
    }

    @Test
    void addingAnActivityLineItemFailsForAnItineraryOwnedByAnotherConsultantBOK07() {
        UUID itineraryId = UUID.randomUUID();
        UUID ownerConsultantId = UUID.randomUUID();
        Itinerary itinerary = new Itinerary(itineraryId, ownerConsultantId, null);
        when(itineraryRepository.findById(itineraryId)).thenReturn(Optional.of(itinerary));
        authenticateAs(Role.CONSULTANT, UUID.randomUUID());

        var command = new AddActivityLineItemCommand(SupplierId.HBACTIVITIES, "activity-1", 120,
            java.time.LocalTime.of(9, 0), 4, new Money(BigDecimal.valueOf(2000), CurrencyCode.INR), CurrencyCode.INR,
            BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO);

        assertThatThrownBy(() -> service.addActivityLineItem(itineraryId, command))
            .isInstanceOf(AccessDeniedException.class);
        verify(activityLineItemRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void addingAnActivityLineItemRejectsAnItineraryThatIsAlreadyAQuotationBOK07() {
        UUID itineraryId = UUID.randomUUID();
        UUID consultantId = UUID.randomUUID();
        Itinerary itinerary = new Itinerary(itineraryId, consultantId, null);
        itinerary.markAsQuotation();
        when(itineraryRepository.findById(itineraryId)).thenReturn(Optional.of(itinerary));
        authenticateAs(Role.CONSULTANT, consultantId);

        var command = new AddActivityLineItemCommand(SupplierId.HBACTIVITIES, "activity-1", 120,
            java.time.LocalTime.of(9, 0), 4, new Money(BigDecimal.valueOf(2000), CurrencyCode.INR), CurrencyCode.INR,
            BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO);

        assertThatThrownBy(() -> service.addActivityLineItem(itineraryId, command))
            .isInstanceOf(IllegalStateException.class);
        verify(activityLineItemRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void updatingActivityHeadcountSucceedsWhileTheItineraryIsStillDraftBOK07() {
        UUID itineraryId = UUID.randomUUID();
        UUID lineItemId = UUID.randomUUID();
        UUID consultantId = UUID.randomUUID();
        Itinerary itinerary = new Itinerary(itineraryId, consultantId, null);
        when(itineraryRepository.findById(itineraryId)).thenReturn(Optional.of(itinerary));
        authenticateAs(Role.CONSULTANT, consultantId);
        ActivityLineItem lineItem = new ActivityLineItem(lineItemId, itineraryId, SupplierId.HBACTIVITIES,
            "activity-1", 120, java.time.LocalTime.of(9, 0), 4, BigDecimal.valueOf(2000), CurrencyCode.INR,
            BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.valueOf(2000), CurrencyCode.INR, BigDecimal.ONE);
        when(activityLineItemRepository.findById(lineItemId)).thenReturn(Optional.of(lineItem));

        service.updateActivityHeadcount(itineraryId, lineItemId, 6);

        assertThat(lineItem.getHeadcount()).isEqualTo(6);
        verify(activityLineItemRepository).save(lineItem);
    }

    @Test
    void updatingActivityHeadcountIsBlockedOnceTheItineraryHasLeftDraftBOK07() {
        UUID itineraryId = UUID.randomUUID();
        UUID lineItemId = UUID.randomUUID();
        UUID consultantId = UUID.randomUUID();
        Itinerary itinerary = new Itinerary(itineraryId, consultantId, null);
        itinerary.markAsQuotation();
        when(itineraryRepository.findById(itineraryId)).thenReturn(Optional.of(itinerary));
        authenticateAs(Role.CONSULTANT, consultantId);

        assertThatThrownBy(() -> service.updateActivityHeadcount(itineraryId, lineItemId, 6))
            .isInstanceOf(IllegalStateException.class);
        verify(activityLineItemRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void consolidateCheckoutTotalSumsSameCurrencyLineItemsWithoutConversionBOK17() {
        UUID itineraryId = UUID.randomUUID();
        HotelLineItem hotel = new HotelLineItem(UUID.randomUUID(), itineraryId, SupplierId.HOTELBEDS, "r1",
            "Taj", "Deluxe", MealPlan.BB, Instant.now(), BigDecimal.valueOf(100), CurrencyCode.INR,
            BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.valueOf(1000), CurrencyCode.INR, BigDecimal.ONE);
        FlightLineItem flight = new FlightLineItem(UUID.randomUUID(), itineraryId, SupplierId.MYSTIFLY, "f1",
            "AI", "AI101", CabinClass.ECONOMY, "23kg", BigDecimal.valueOf(100), CurrencyCode.INR,
            BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.valueOf(500), CurrencyCode.INR, BigDecimal.ONE);
        when(hotelLineItemRepository.findByItineraryId(itineraryId)).thenReturn(List.of(hotel));
        when(flightLineItemRepository.findByItineraryId(itineraryId)).thenReturn(List.of(flight));

        Money total = service.consolidateCheckoutTotal(
            new ConsolidateCheckoutTotalCommand(itineraryId, CurrencyCode.INR, Map.of()));

        assertThat(total).isEqualTo(new Money(BigDecimal.valueOf(1500), CurrencyCode.INR));
    }

    @Test
    void consolidateCheckoutTotalConvertsAMixedCurrencyLineItemUsingTheSuppliedRateBOK17() {
        UUID itineraryId = UUID.randomUUID();
        HotelLineItem inrHotel = new HotelLineItem(UUID.randomUUID(), itineraryId, SupplierId.HOTELBEDS, "r1",
            "Taj", "Deluxe", MealPlan.BB, Instant.now(), BigDecimal.valueOf(100), CurrencyCode.INR,
            BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.valueOf(1000), CurrencyCode.INR, BigDecimal.ONE);
        // A BYOS supplier's line item priced (sold) in AED, not INR — the
        // exact PRD §23.1 Edge Case #2 scenario.
        HotelLineItem aedHotel = new HotelLineItem(UUID.randomUUID(), itineraryId, SupplierId.BYOS, "r2",
            "Burj Suite", "Suite", MealPlan.BB, Instant.now(), BigDecimal.valueOf(100), CurrencyCode.AED,
            BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.valueOf(100), CurrencyCode.AED, BigDecimal.ONE);
        when(hotelLineItemRepository.findByItineraryId(itineraryId)).thenReturn(List.of(inrHotel, aedHotel));

        Money total = service.consolidateCheckoutTotal(new ConsolidateCheckoutTotalCommand(
            itineraryId, CurrencyCode.INR, Map.of(CurrencyCode.AED, BigDecimal.valueOf(23))));

        // 1000 INR + (100 AED * 23) = 1000 + 2300 = 3300 INR — one
        // consolidated total, never a mixed-currency figure.
        assertThat(total).isEqualTo(new Money(BigDecimal.valueOf(3300), CurrencyCode.INR));
    }

    @Test
    void consolidateCheckoutTotalFailsLoudlyWhenARequiredConversionRateIsMissingBOK17() {
        UUID itineraryId = UUID.randomUUID();
        HotelLineItem aedHotel = new HotelLineItem(UUID.randomUUID(), itineraryId, SupplierId.BYOS, "r2",
            "Burj Suite", "Suite", MealPlan.BB, Instant.now(), BigDecimal.valueOf(100), CurrencyCode.AED,
            BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.valueOf(100), CurrencyCode.AED, BigDecimal.ONE);
        when(hotelLineItemRepository.findByItineraryId(itineraryId)).thenReturn(List.of(aedHotel));

        assertThatThrownBy(() -> service.consolidateCheckoutTotal(
            new ConsolidateCheckoutTotalCommand(itineraryId, CurrencyCode.INR, Map.of())))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void recalculateQuotationUpdatesTravelerCountAndPublishesTheEventBOK18() {
        UUID consultantId = UUID.randomUUID();
        UUID quotationId = stubQuotationResolvingTo(consultantId);
        authenticateAs(Role.CONSULTANT, consultantId);

        service.recalculateQuotation(quotationId, 5);

        ArgumentCaptor<QuotationRecalculatedEvent> captor = ArgumentCaptor.forClass(QuotationRecalculatedEvent.class);
        verify(events).publishEvent(captor.capture());
        assertThat(captor.getValue().quotationId()).isEqualTo(quotationId);
        assertThat(captor.getValue().newTravelerCount()).isEqualTo(5);
    }

    @Test
    void recalculateQuotationIsBlockedOnceTheItineraryIsBookedBOK18() {
        UUID itineraryId = UUID.randomUUID();
        UUID quotationId = UUID.randomUUID();
        UUID consultantId = UUID.randomUUID();
        Itinerary bookedItinerary = new Itinerary(itineraryId, consultantId, null);
        bookedItinerary.markAsQuotation();
        bookedItinerary.markAsBooked();
        when(quotationRepository.findById(quotationId)).thenReturn(
            Optional.of(new Quotation(quotationId, itineraryId, Instant.now().plusSeconds(3600))));
        when(itineraryRepository.findById(itineraryId)).thenReturn(Optional.of(bookedItinerary));
        authenticateAs(Role.CONSULTANT, consultantId);

        assertThatThrownBy(() -> service.recalculateQuotation(quotationId, 5))
            .isInstanceOf(IllegalStateException.class);
        verify(events, org.mockito.Mockito.never()).publishEvent(any(QuotationRecalculatedEvent.class));
    }

    @Test
    void recalculateQuotationFailsForAnItineraryOwnedByAnotherConsultantBOK18() {
        UUID ownerConsultantId = UUID.randomUUID();
        UUID quotationId = stubQuotationResolvingTo(ownerConsultantId);
        authenticateAs(Role.CONSULTANT, UUID.randomUUID());

        assertThatThrownBy(() -> service.recalculateQuotation(quotationId, 5))
            .isInstanceOf(AccessDeniedException.class);
        verify(events, org.mockito.Mockito.never()).publishEvent(any(QuotationRecalculatedEvent.class));
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
    void convertingAQuotationToAPackageDetectsADynamicFlightHotelComboBOK11() {
        UUID itineraryId = UUID.randomUUID();
        UUID consultantId = UUID.randomUUID();
        UUID quotationId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, consultantId);
        when(quotationRepository.findById(quotationId)).thenReturn(
            Optional.of(new Quotation(quotationId, itineraryId, Instant.now().plusSeconds(3600))));
        when(itineraryRepository.findById(itineraryId)).thenReturn(
            Optional.of(new Itinerary(itineraryId, consultantId, null)));
        HotelLineItem hotel = new HotelLineItem(UUID.randomUUID(), itineraryId, SupplierId.HOTELBEDS,
            "rate-key-1", "Taj Palace", "Deluxe Room", MealPlan.BB, Instant.now().plusSeconds(3600),
            BigDecimal.valueOf(100), CurrencyCode.INR, BigDecimal.ZERO, BigDecimal.ZERO,
            BigDecimal.valueOf(5000), CurrencyCode.INR, BigDecimal.ONE);
        when(hotelLineItemRepository.findByItineraryId(itineraryId)).thenReturn(List.of(hotel));
        FlightLineItem flight = new FlightLineItem(UUID.randomUUID(), itineraryId, SupplierId.MYSTIFLY, "f1",
            "AI", "AI101", CabinClass.ECONOMY, "23kg", BigDecimal.valueOf(100), CurrencyCode.INR,
            BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.valueOf(2000), CurrencyCode.INR, BigDecimal.ONE);
        when(flightLineItemRepository.findByItineraryId(itineraryId)).thenReturn(List.of(flight));
        var command = new ConvertQuotationToPackageCommand("UK Escape", null,
            LocalDate.now().plusDays(30), LocalDate.now().plusDays(90), BigDecimal.valueOf(500), 2);

        service.convertQuotationToPackage(quotationId, command);

        ArgumentCaptor<TravelPackage> captor = ArgumentCaptor.forClass(TravelPackage.class);
        verify(travelPackageRepository).save(captor.capture());
        assertThat(captor.getValue().isDynamicFlightHotelCombo()).isTrue();
    }

    @Test
    void convertingAHotelOnlyQuotationIsNotADynamicComboBOK11() {
        UUID itineraryId = UUID.randomUUID();
        UUID consultantId = UUID.randomUUID();
        UUID quotationId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, consultantId);
        when(quotationRepository.findById(quotationId)).thenReturn(
            Optional.of(new Quotation(quotationId, itineraryId, Instant.now().plusSeconds(3600))));
        when(itineraryRepository.findById(itineraryId)).thenReturn(
            Optional.of(new Itinerary(itineraryId, consultantId, null)));
        HotelLineItem hotel = new HotelLineItem(UUID.randomUUID(), itineraryId, SupplierId.HOTELBEDS,
            "rate-key-1", "Taj Palace", "Deluxe Room", MealPlan.BB, Instant.now().plusSeconds(3600),
            BigDecimal.valueOf(100), CurrencyCode.INR, BigDecimal.ZERO, BigDecimal.ZERO,
            BigDecimal.valueOf(5000), CurrencyCode.INR, BigDecimal.ONE);
        when(hotelLineItemRepository.findByItineraryId(itineraryId)).thenReturn(List.of(hotel));
        var command = new ConvertQuotationToPackageCommand("Goa Getaway", null,
            LocalDate.now().plusDays(30), LocalDate.now().plusDays(90), BigDecimal.valueOf(500), 2);

        service.convertQuotationToPackage(quotationId, command);

        ArgumentCaptor<TravelPackage> captor = ArgumentCaptor.forClass(TravelPackage.class);
        verify(travelPackageRepository).save(captor.capture());
        assertThat(captor.getValue().isDynamicFlightHotelCombo()).isFalse();
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
    void publishingAUkDynamicComboPackageIsBlockedWithoutAtolDisclosureBOK11() {
        UUID packageId = UUID.randomUUID();
        UUID consultantId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, consultantId);
        TravelPackage travelPackage = new TravelPackage(packageId, UUID.randomUUID(), consultantId, "UK Escape",
            null, LocalDate.now().plusDays(30), LocalDate.now().plusDays(90),
            BigDecimal.valueOf(7000), BigDecimal.valueOf(500), CurrencyCode.GBP, 2);
        travelPackage.markDynamicFlightHotelCombo();
        when(travelPackageRepository.findById(packageId)).thenReturn(Optional.of(travelPackage));
        when(whitelabelApi.findConsultantMarket(consultantId)).thenReturn(Market.UK);

        assertThatThrownBy(() -> service.publishPackage(packageId, false))
            .isInstanceOf(com.adren.travel.booking.AtolDisclosureRequiredException.class);
        assertThat(travelPackage.getStatus()).isEqualTo(PackageStatus.DRAFT);
        verify(travelPackageRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void publishingAUkDynamicComboPackageSucceedsAfterAtolDisclosureCompletedBOK11() {
        UUID packageId = UUID.randomUUID();
        UUID consultantId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, consultantId);
        TravelPackage travelPackage = new TravelPackage(packageId, UUID.randomUUID(), consultantId, "UK Escape",
            null, LocalDate.now().plusDays(30), LocalDate.now().plusDays(90),
            BigDecimal.valueOf(7000), BigDecimal.valueOf(500), CurrencyCode.GBP, 2);
        travelPackage.markDynamicFlightHotelCombo();
        travelPackage.completeAtolDisclosure();
        when(travelPackageRepository.findById(packageId)).thenReturn(Optional.of(travelPackage));
        when(whitelabelApi.findConsultantMarket(consultantId)).thenReturn(Market.UK);

        service.publishPackage(packageId, false);

        assertThat(travelPackage.getStatus()).isEqualTo(PackageStatus.PUBLISHED);
    }

    @Test
    void publishingANonUkDynamicComboPackageIsNotGatedByAtolDisclosureBOK11() {
        UUID packageId = UUID.randomUUID();
        UUID consultantId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, consultantId);
        TravelPackage travelPackage = new TravelPackage(packageId, UUID.randomUUID(), consultantId, "India Getaway",
            null, LocalDate.now().plusDays(30), LocalDate.now().plusDays(90),
            BigDecimal.valueOf(7000), BigDecimal.valueOf(500), CurrencyCode.INR, 2);
        travelPackage.markDynamicFlightHotelCombo();
        when(travelPackageRepository.findById(packageId)).thenReturn(Optional.of(travelPackage));
        when(whitelabelApi.findConsultantMarket(consultantId)).thenReturn(Market.INDIA);

        service.publishPackage(packageId, false);

        assertThat(travelPackage.getStatus()).isEqualTo(PackageStatus.PUBLISHED);
    }

    @Test
    void completingAtolDisclosureSetsTheFlagBOK11() {
        UUID packageId = UUID.randomUUID();
        UUID consultantId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, consultantId);
        TravelPackage travelPackage = new TravelPackage(packageId, UUID.randomUUID(), consultantId, "UK Escape",
            null, LocalDate.now().plusDays(30), LocalDate.now().plusDays(90),
            BigDecimal.valueOf(7000), BigDecimal.valueOf(500), CurrencyCode.GBP, 2);
        when(travelPackageRepository.findById(packageId)).thenReturn(Optional.of(travelPackage));

        service.completeAtolDisclosure(packageId);

        assertThat(travelPackage.isAtolDisclosureCompleted()).isTrue();
        verify(travelPackageRepository).save(travelPackage);
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
