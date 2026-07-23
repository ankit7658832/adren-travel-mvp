package com.adren.travel.booking.internal;

import com.adren.travel.ai.AiApi;
import com.adren.travel.ai.AiItineraryGenerationResult;
import com.adren.travel.ai.AiItinerarySuggestion;
import com.adren.travel.ai.AiPricingRevalidationResult;
import com.adren.travel.ai.PricingStale;
import com.adren.travel.booking.AddActivityLineItemCommand;
import com.adren.travel.booking.AiPricingStaleException;
import com.adren.travel.booking.AllConsultantGmvView;
import com.adren.travel.booking.ApproveAiSuggestionCommand;
import com.adren.travel.booking.AddCruiseLineItemCommand;
import com.adren.travel.booking.AddFlightLineItemCommand;
import com.adren.travel.booking.AddHotelLineItemCommand;
import com.adren.travel.booking.AddTransferLineItemCommand;
import com.adren.travel.booking.AlternateOption;
import com.adren.travel.booking.BookingApi;
import com.adren.travel.booking.BookingSearchResultView;
import com.adren.travel.booking.BookingView;
import com.adren.travel.booking.CalculateCancellationRefundCommand;
import com.adren.travel.booking.CancellationRequestView;
import com.adren.travel.booking.ConsolidateCheckoutTotalCommand;
import com.adren.travel.booking.ConsultantBookingMetricsView;
import com.adren.travel.booking.ConvertQuotationToPackageCommand;
import com.adren.travel.booking.CreateTravelerProfileCommand;
import com.adren.travel.booking.DisputeTicketView;
import com.adren.travel.booking.FlagDisputeCommand;
import com.adren.travel.booking.GenerateAiSuggestionCommand;
import com.adren.travel.booking.PackageSummaryView;
import com.adren.travel.booking.PackageView;
import com.adren.travel.booking.QuotationSummaryView;
import com.adren.travel.booking.SupplierPerformanceView;
import com.adren.travel.booking.VoucherView;
import com.adren.travel.booking.event.ActivityLineItemAddedEvent;
import com.adren.travel.booking.event.BookingCancelledEvent;
import com.adren.travel.booking.event.BookingConfirmedEvent;
import com.adren.travel.booking.event.CruiseLineItemAddedEvent;
import com.adren.travel.booking.event.DisputeTicketCreatedEvent;
import com.adren.travel.booking.event.FlightLineItemAddedEvent;
import com.adren.travel.booking.event.HotelLineItemAddedEvent;
import com.adren.travel.booking.event.ItineraryQuotationSavedEvent;
import com.adren.travel.booking.InventoryNoLongerAvailableException;
import com.adren.travel.booking.event.TransferLineItemAddedEvent;
import com.adren.travel.booking.event.PackageCreatedEvent;
import com.adren.travel.booking.event.PackagePriceChangedEvent;
import com.adren.travel.booking.event.PackagePublishedEvent;
import com.adren.travel.booking.event.QuotationRecalculatedEvent;
import com.adren.travel.booking.event.TravelerProfileCreatedEvent;
import com.adren.travel.payments.CalculateRefundCommand;
import com.adren.travel.payments.CalculateSellRateCommand;
import com.adren.travel.payments.PaymentsApi;
import com.adren.travel.payments.RefundCalculation;
import com.adren.travel.payments.SellRateCalculation;
import com.adren.travel.payments.WalletHoldCommand;
import com.adren.travel.security.CurrentPrincipal;
import com.adren.travel.shared.CurrencyAmount;
import com.adren.travel.shared.CurrencyCode;
import com.adren.travel.shared.Money;
import com.adren.travel.shared.ProductCategory;
import com.adren.travel.supplier.SupplierId;
import com.adren.travel.supplier.SupplierSearchApi;
import com.adren.travel.supplier.SupplierSearchResult;
import com.adren.travel.whitelabel.Market;
import com.adren.travel.whitelabel.WhitelabelApi;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

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

    // BOK-09 — PRD §20.9 gives no explicit default validity duration for
    // valid_until; 7 days is a documented assumption pending business
    // input, not a value derived from any PRD section.
    private static final Duration QUOTATION_VALIDITY_WINDOW = Duration.ofDays(7);

    private final ItineraryRepository itineraryRepository;
    private final TravelerProfileRepository travelerProfileRepository;
    private final HotelLineItemRepository hotelLineItemRepository;
    private final FlightLineItemRepository flightLineItemRepository;
    private final TransferLineItemRepository transferLineItemRepository;
    private final CruiseLineItemRepository cruiseLineItemRepository;
    private final ActivityLineItemRepository activityLineItemRepository;
    private final QuotationRepository quotationRepository;
    private final TravelPackageRepository travelPackageRepository;
    private final BookingRepository bookingRepository;
    private final VoucherService voucherService;
    private final VoucherRepository voucherRepository;
    private final ApplicationEventPublisher events;
    private final WhitelabelApi whitelabelApi;
    private final SupplierSearchApi supplierSearchApi;
    private final HotelDedupService hotelDedupService;
    private final PaymentsApi paymentsApi;
    private final CancellationRequestRepository cancellationRequestRepository;
    private final DisputeTicketRepository disputeTicketRepository;
    private final AiApi aiApi;

    // backend-best-practices §4 flags >4-5 constructor dependencies as a
    // decomposition signal — this one is well past that (pre-existing,
    // grew one line-item repo at a time across BOK-03..07). Not refactored
    // here (AI-02 is a functional addition, not the moment for that
    // separate change) but worth flagging rather than silently adding
    // another param without comment.
    BookingServiceImpl(ItineraryRepository itineraryRepository, TravelerProfileRepository travelerProfileRepository,
                        HotelLineItemRepository hotelLineItemRepository, FlightLineItemRepository flightLineItemRepository,
                        TransferLineItemRepository transferLineItemRepository,
                        CruiseLineItemRepository cruiseLineItemRepository,
                        ActivityLineItemRepository activityLineItemRepository,
                        QuotationRepository quotationRepository,
                        TravelPackageRepository travelPackageRepository, BookingRepository bookingRepository,
                        VoucherService voucherService, VoucherRepository voucherRepository,
                        ApplicationEventPublisher events, WhitelabelApi whitelabelApi,
                        SupplierSearchApi supplierSearchApi, HotelDedupService hotelDedupService,
                        PaymentsApi paymentsApi, CancellationRequestRepository cancellationRequestRepository,
                        DisputeTicketRepository disputeTicketRepository, AiApi aiApi) {
        this.itineraryRepository = itineraryRepository;
        this.travelerProfileRepository = travelerProfileRepository;
        this.hotelLineItemRepository = hotelLineItemRepository;
        this.flightLineItemRepository = flightLineItemRepository;
        this.transferLineItemRepository = transferLineItemRepository;
        this.cruiseLineItemRepository = cruiseLineItemRepository;
        this.activityLineItemRepository = activityLineItemRepository;
        this.quotationRepository = quotationRepository;
        this.travelPackageRepository = travelPackageRepository;
        this.bookingRepository = bookingRepository;
        this.voucherService = voucherService;
        this.voucherRepository = voucherRepository;
        this.events = events;
        this.whitelabelApi = whitelabelApi;
        this.hotelDedupService = hotelDedupService;
        this.aiApi = aiApi;
        this.supplierSearchApi = supplierSearchApi;
        this.paymentsApi = paymentsApi;
        this.cancellationRequestRepository = cancellationRequestRepository;
        this.disputeTicketRepository = disputeTicketRepository;
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

        // BOK-08 AC: "at least one line item per required category" —
        // narrowed to "at least one line item of any of the five supported
        // types" now that BOK-04..07 all exist (was Hotel-only when only
        // BOK-03 existed).
        boolean hasAnyLineItem = !hotelLineItemRepository.findByItineraryId(itineraryId).isEmpty()
            || !flightLineItemRepository.findByItineraryId(itineraryId).isEmpty()
            || !transferLineItemRepository.findByItineraryId(itineraryId).isEmpty()
            || !cruiseLineItemRepository.findByItineraryId(itineraryId).isEmpty()
            || !activityLineItemRepository.findByItineraryId(itineraryId).isEmpty();
        if (!hasAnyLineItem) {
            throw new IllegalStateException("Cannot save as quotation: itinerary " + itineraryId + " has no line items");
        }

        itinerary.markAsQuotation();
        itineraryRepository.save(itinerary);

        UUID quotationId = UUID.randomUUID();
        Instant validUntil = Instant.now().plus(QUOTATION_VALIDITY_WINDOW);
        quotationRepository.save(new Quotation(quotationId, itineraryId, validUntil));

        events.publishEvent(new ItineraryQuotationSavedEvent(itineraryId, quotationId, itinerary.getConsultantId()));
        return quotationId;
    }

    @Override
    @Transactional
    public AiItineraryGenerationResult generateAiItinerarySuggestion(UUID itineraryId, GenerateAiSuggestionCommand command) {
        Itinerary itinerary = requireOwnedDraftItinerary(itineraryId);

        AiItineraryGenerationResult result = aiApi.generateItinerary(new com.adren.travel.ai.GenerateItineraryCommand(
            itinerary.getConsultantId(), itineraryId, command.locationCode(), command.checkIn(), command.checkOut(),
            command.naturalLanguageRequest(), command.budgetLimit(), false));

        // AI-06's approval gate (Itinerary.markAsQuotation) checks
        // aiGenerated/aiApproved — only record aiGenerated on an actual
        // suggestion, never on a NoViableSuggestion outcome (nothing was
        // suggested, so nothing needs approval before Quotation).
        if (result instanceof AiItinerarySuggestion suggestion) {
            itinerary.markAiGenerated(suggestion.auditLogId());
            itineraryRepository.save(itinerary);
        }
        return result;
    }

    @Override
    @Transactional
    public AiItineraryGenerationResult completeItineraryWithAi(UUID itineraryId, GenerateAiSuggestionCommand command) {
        Itinerary itinerary = requireOwnedDraftItinerary(itineraryId);

        // AI-03: the only signal the ai module needs to respect an
        // existing selection — computed here since booking is the only
        // module that can see the itinerary's line items.
        boolean hasExistingHotelSelection = !hotelLineItemRepository.findByItineraryId(itineraryId).isEmpty();

        AiItineraryGenerationResult result = aiApi.generateItinerary(new com.adren.travel.ai.GenerateItineraryCommand(
            itinerary.getConsultantId(), itineraryId, command.locationCode(), command.checkIn(), command.checkOut(),
            command.naturalLanguageRequest(), command.budgetLimit(), hasExistingHotelSelection));

        if (result instanceof AiItinerarySuggestion suggestion) {
            itinerary.markAiGenerated(suggestion.auditLogId());
            itineraryRepository.save(itinerary);
        }
        return result;
    }

    @Override
    @Transactional
    public void approveAiSuggestion(UUID itineraryId, ApproveAiSuggestionCommand command) {
        Itinerary itinerary = requireOwnedDraftItinerary(itineraryId);
        UUID approvedByUserId = CurrentPrincipal.get().userId();
        aiApi.approveAiSuggestion(new com.adren.travel.ai.ApproveAiSuggestionCommand(
            command.auditLogId(), approvedByUserId, command.finalLineItems()));
        itinerary.markAiApproved();
        itineraryRepository.save(itinerary);
    }

    @Override
    @Transactional
    public UUID confirmBooking(UUID quotationOrPackageId, Money totalSellPrice) {
        ConfirmationTarget target = resolveConfirmationTargetFor(quotationOrPackageId);
        CurrentPrincipal.resolveTenantScope(target.consultantId());
        requireActiveUnlessSuperAdmin(target.consultantId());

        // AI-09, PRD §11.3: re-validate BEFORE the concurrency lock — no
        // point winning the booking race only to fail on stale pricing.
        requireFreshAiPricingIfApplicable(target.itineraryId());

        // BOK-16, PRD §23.1 Edge Case #1: two concurrent confirmBooking
        // calls for the SAME itinerary must not both succeed — this
        // saveAndFlush is where that race is actually decided (see
        // Itinerary's @Version Javadoc), before any wallet debit happens.
        lockForBooking(target.itineraryId());

        UUID bookingId = UUID.randomUUID();

        // FIN-07: this direct (non-Stripe) confirmBooking path is the
        // wallet payment method — Stripe payments instead go through
        // confirmBookingFromPaymentWebhook, and On-Account payments through
        // confirmBookingOnAccount (FIN-12); neither of those touches the
        // wallet balance/credit the way this path does. Placing then
        // immediately resolving the hold in the same call is a
        // simplification: this scaffold has no separate "reach payment
        // step" moment distinct from confirmation itself, unlike the full
        // booking flow PRD §12.3 describes.
        paymentsApi.placeHold(new WalletHoldCommand(bookingId, target.consultantId(), totalSellPrice));
        paymentsApi.resolveHoldAsDebit(new WalletHoldCommand(bookingId, target.consultantId(), totalSellPrice));

        return finalizeConfirmedBooking(
            bookingId, target.itineraryId(), target.consultantId(), totalSellPrice, PaymentMethod.WALLET);
    }

    @Override
    @Transactional
    public UUID confirmBookingOnAccount(UUID quotationOrPackageId, Money totalSellPrice) {
        ConfirmationTarget target = resolveConfirmationTargetFor(quotationOrPackageId);
        CurrentPrincipal.resolveTenantScope(target.consultantId());
        requireActiveUnlessSuperAdmin(target.consultantId());

        // AI-09, PRD §11.3: same re-validation as the wallet path — the
        // payment method never changes whether stale AI pricing blocks
        // confirmation.
        requireFreshAiPricingIfApplicable(target.itineraryId());

        // BOK-16, PRD §23.1 Edge Case #1: same concurrency guard as the
        // wallet path — the last available inventory unit can't be
        // double-booked regardless of payment method.
        lockForBooking(target.itineraryId());

        UUID bookingId = UUID.randomUUID();

        // FIN-12: On-Account billing — never a wallet hold/debit, never
        // gated by FIN-08's credit-limit check (a separate settlement
        // path, settled later, not against wallet balance/credit).
        paymentsApi.payOnAccount(new WalletHoldCommand(bookingId, target.consultantId(), totalSellPrice));

        return finalizeConfirmedBooking(
            bookingId, target.itineraryId(), target.consultantId(), totalSellPrice, PaymentMethod.ON_ACCOUNT);
    }

    // BOK-13 — quotationOrPackageId is polymorphic: a booking can be
    // confirmed directly from a Quotation or from a published Package.
    private record ConfirmationTarget(UUID consultantId, UUID itineraryId) {
    }

    private ConfirmationTarget resolveConfirmationTargetFor(UUID quotationOrPackageId) {
        return quotationRepository.findById(quotationOrPackageId)
            .map(quotation -> new ConfirmationTarget(itineraryRepository.findById(quotation.getItineraryId())
                .orElseThrow(() -> new IllegalArgumentException("No itinerary: " + quotation.getItineraryId()))
                .getConsultantId(), quotation.getItineraryId()))
            .or(() -> travelPackageRepository.findById(quotationOrPackageId)
                .map(pkg -> new ConfirmationTarget(pkg.getConsultantId(), pkg.getSourceItineraryId())))
            .orElseThrow(() -> new IllegalArgumentException("No quotation or package: " + quotationOrPackageId));
    }

    // BOK-16 — deliberately scoped to confirmBooking (the direct/wallet
    // path). confirmBookingFromPaymentWebhook receives the same
    // quotationOrPackageId shape and could get the identical guard as a
    // follow-up, but it's invoked from an async, unauthenticated listener
    // context (no CurrentPrincipal) already covered by its own dedicated
    // test suite (StripePaymentConfirmationListenerTest,
    // BookingServiceImplTest's confirmBookingFromPaymentWebhook* tests) —
    // extending the lock there is a separate, deliberate change, not
    // bundled into this story.
    private void lockForBooking(UUID itineraryId) {
        Itinerary itinerary = itineraryRepository.findById(itineraryId)
            .orElseThrow(() -> new IllegalArgumentException("No itinerary: " + itineraryId));
        itinerary.markAsBooked();
        try {
            itineraryRepository.saveAndFlush(itinerary);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new InventoryNoLongerAvailableException(itineraryId);
        }
    }

    /**
     * AI-09, PRD §11.3 — a no-op for any itinerary that was never
     * AI-generated ({@code Itinerary.isAiGenerated()} false); scoped to
     * {@code confirmBooking}/{@code confirmBookingOnAccount} same as
     * {@code lockForBooking}'s own scoping note explains for BOK-16 (the
     * Stripe-webhook path runs from an async, unauthenticated listener
     * context this check is a separate, deliberate follow-up for).
     */
    private void requireFreshAiPricingIfApplicable(UUID itineraryId) {
        Itinerary itinerary = itineraryRepository.findById(itineraryId)
            .orElseThrow(() -> new IllegalArgumentException("No itinerary: " + itineraryId));
        if (!itinerary.isAiGenerated()) {
            return;
        }
        AiPricingRevalidationResult result = aiApi.revalidateAiPricingAtBooking(itinerary.getAiAuditLogId());
        if (result instanceof PricingStale stale) {
            throw new AiPricingStaleException(itineraryId, stale.reason());
        }
    }

    @Override
    @Transactional
    public UUID confirmBookingFromPaymentWebhook(UUID quotationOrPackageId, UUID consultantId, Money totalSellPrice) {
        // FIN-07: this Stripe path never touches the wallet — the customer
        // already paid by card, not wallet/credit.
        // BOK-16/BOK-19 scoping note: this path deliberately does not
        // resolve quotationOrPackageId to a real itineraryId (see
        // lockForBooking's Javadoc on why extending the concurrency guard
        // here is a separate change) — the persisted Booking row's
        // itineraryId is left null for bookings confirmed this way.
        UUID bookingId = UUID.randomUUID();
        return finalizeConfirmedBooking(bookingId, null, consultantId, totalSellPrice, PaymentMethod.STRIPE);
    }

    private UUID finalizeConfirmedBooking(UUID bookingId, UUID itineraryId, UUID consultantId, Money totalSellPrice,
                                           PaymentMethod paymentMethod) {
        // BOK-19: a fresh PNR is vanishingly unlikely to collide (32^8
        // possibilities), but the unique constraint means a collision is
        // detected, not silently overwritten — retry rather than trust luck.
        String pnrSearchableRef = PnrGenerator.generate();
        while (bookingRepository.existsByPnrSearchableRef(pnrSearchableRef)) {
            pnrSearchableRef = PnrGenerator.generate();
        }
        bookingRepository.save(new Booking(bookingId, itineraryId, consultantId, totalSellPrice.amount(),
            totalSellPrice.currency(), paymentMethod, pnrSearchableRef));

        // BOK-15: voucher generation happens synchronously, in the SAME
        // transactional scope as the booking confirmation itself — unlike
        // notification (deliberately async/fire-and-forget), a voucher is
        // part of what "confirmed" means, not a side effect of it.
        voucherService.generateFor(bookingId);
        events.publishEvent(new BookingConfirmedEvent(bookingId, consultantId, totalSellPrice));
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
        // BOK-20: this alternate-selection panel is the actual "browse all
        // options" surface a Consultant scans for duplicate listings — more
        // visibly so than GeocodeAndSearchService's single auto-selected
        // pin, so dedup applies here too, not only ahead of Default
        // Selection.
        List<SupplierSearchResult> rawOptions = supplierSearchApi.searchHotels(locationCode, checkIn, checkOut);
        return hotelDedupService.deduplicate(rawOptions).stream()
            .map(result -> new AlternateOption(result.supplierId().name(), result.supplierRateId(),
                result.propertyName(), result.roomType(), result.netRate().amount(), result.netRate().currency(),
                result.rating()))
            .toList();
    }

    @Override
    @Transactional
    public UUID createTravelerProfile(CreateTravelerProfileCommand command) {
        UUID consultantId = CurrentPrincipal.get().consultantId();
        UUID travelerId = UUID.randomUUID();
        List<String> documentVaultReferences =
            command.documentVaultReferences() != null ? command.documentVaultReferences() : List.of();
        Map<String, String> preferences = command.preferences() != null ? command.preferences() : Map.of();

        TravelerProfile profile = new TravelerProfile(travelerId, consultantId, command.name(), command.dateOfBirth(),
            command.passportNumber(), command.passportExpiry(), command.nationality(), documentVaultReferences, preferences);
        travelerProfileRepository.save(profile);

        events.publishEvent(new TravelerProfileCreatedEvent(travelerId, consultantId));
        return travelerId;
    }

    @Override
    @Transactional
    public UUID addHotelLineItem(UUID itineraryId, AddHotelLineItemCommand command) {
        Itinerary itinerary = requireOwnedDraftItinerary(itineraryId);

        UUID lineItemId = UUID.randomUUID();
        SellRateCalculation priced = paymentsApi.calculateSellRate(new CalculateSellRateCommand(
            lineItemId, itinerary.getConsultantId(), ProductCategory.HOTEL, command.netRate(),
            command.sellCurrency(), command.fxRate(), command.bufferPercent(), command.commissionPercent()));

        HotelLineItem lineItem = new HotelLineItem(lineItemId, itineraryId, command.supplierId(),
            command.supplierRateId(), command.propertyName(), command.roomType(), command.mealPlan(),
            command.cancellationDeadline(), command.netRate().amount(), command.netRate().currency(),
            priced.markupAmount().amount(), priced.bufferedAmount().amount().subtract(priced.fxConvertedBase().amount()),
            priced.sellRate().amount(), priced.sellRate().currency(), priced.fxRateSnapshot().rate());
        hotelLineItemRepository.save(lineItem);

        events.publishEvent(new HotelLineItemAddedEvent(lineItemId, itineraryId, itinerary.getConsultantId(),
            priced.sellRate()));
        return lineItemId;
    }

    @Override
    @Transactional
    public UUID addFlightLineItem(UUID itineraryId, AddFlightLineItemCommand command) {
        Itinerary itinerary = requireOwnedDraftItinerary(itineraryId);

        UUID lineItemId = UUID.randomUUID();
        SellRateCalculation priced = paymentsApi.calculateSellRate(new CalculateSellRateCommand(
            lineItemId, itinerary.getConsultantId(), ProductCategory.FLIGHT, command.netRate(),
            command.sellCurrency(), command.fxRate(), command.bufferPercent(), command.commissionPercent()));

        FlightLineItem lineItem = new FlightLineItem(lineItemId, itineraryId, command.supplierId(),
            command.supplierRateId(), command.airlineCode(), command.flightNumber(), command.cabinClass(),
            command.baggageAllowance(), command.netRate().amount(), command.netRate().currency(),
            priced.markupAmount().amount(), priced.bufferedAmount().amount().subtract(priced.fxConvertedBase().amount()),
            priced.sellRate().amount(), priced.sellRate().currency(), priced.fxRateSnapshot().rate());
        flightLineItemRepository.save(lineItem);

        events.publishEvent(new FlightLineItemAddedEvent(lineItemId, itineraryId, itinerary.getConsultantId(),
            priced.sellRate()));
        return lineItemId;
    }

    @Override
    @Transactional
    public UUID addTransferLineItem(UUID itineraryId, AddTransferLineItemCommand command) {
        Itinerary itinerary = requireOwnedDraftItinerary(itineraryId);

        UUID lineItemId = UUID.randomUUID();
        SellRateCalculation priced = paymentsApi.calculateSellRate(new CalculateSellRateCommand(
            lineItemId, itinerary.getConsultantId(), ProductCategory.TRANSFER, command.netRate(),
            command.sellCurrency(), command.fxRate(), command.bufferPercent(), command.commissionPercent()));

        TransferLineItem lineItem = new TransferLineItem(lineItemId, itineraryId, command.supplierId(),
            command.supplierRateId(), command.vehicleType(), command.pickupPoint(), command.dropoffPoint(),
            command.netRate().amount(), command.netRate().currency(), priced.markupAmount().amount(),
            priced.bufferedAmount().amount().subtract(priced.fxConvertedBase().amount()),
            priced.sellRate().amount(), priced.sellRate().currency(), priced.fxRateSnapshot().rate());
        transferLineItemRepository.save(lineItem);

        events.publishEvent(new TransferLineItemAddedEvent(lineItemId, itineraryId, itinerary.getConsultantId(),
            priced.sellRate()));
        return lineItemId;
    }

    @Override
    @Transactional
    public UUID addCruiseLineItem(UUID itineraryId, AddCruiseLineItemCommand command) {
        Itinerary itinerary = requireOwnedDraftItinerary(itineraryId);

        UUID lineItemId = UUID.randomUUID();
        SellRateCalculation priced = paymentsApi.calculateSellRate(new CalculateSellRateCommand(
            lineItemId, itinerary.getConsultantId(), ProductCategory.CRUISE, command.netRate(),
            command.sellCurrency(), command.fxRate(), command.bufferPercent(), command.commissionPercent()));

        CruiseLineItem lineItem = new CruiseLineItem(lineItemId, itineraryId, command.supplierId(),
            command.supplierRateId(), command.cruiseLine(), command.cabinCategory(), command.ports(),
            command.passengerDocumentsRequired(), command.netRate().amount(), command.netRate().currency(),
            priced.markupAmount().amount(), priced.bufferedAmount().amount().subtract(priced.fxConvertedBase().amount()),
            priced.sellRate().amount(), priced.sellRate().currency(), priced.fxRateSnapshot().rate());
        cruiseLineItemRepository.save(lineItem);

        events.publishEvent(new CruiseLineItemAddedEvent(lineItemId, itineraryId, itinerary.getConsultantId(),
            priced.sellRate()));
        return lineItemId;
    }

    @Override
    @Transactional
    public UUID addActivityLineItem(UUID itineraryId, AddActivityLineItemCommand command) {
        Itinerary itinerary = requireOwnedDraftItinerary(itineraryId);

        UUID lineItemId = UUID.randomUUID();
        SellRateCalculation priced = paymentsApi.calculateSellRate(new CalculateSellRateCommand(
            lineItemId, itinerary.getConsultantId(), ProductCategory.ACTIVITY, command.netRate(),
            command.sellCurrency(), command.fxRate(), command.bufferPercent(), command.commissionPercent()));

        ActivityLineItem lineItem = new ActivityLineItem(lineItemId, itineraryId, command.supplierId(),
            command.supplierRateId(), command.durationMinutes(), command.timeSlot(), command.headcount(),
            command.netRate().amount(), command.netRate().currency(), priced.markupAmount().amount(),
            priced.bufferedAmount().amount().subtract(priced.fxConvertedBase().amount()),
            priced.sellRate().amount(), priced.sellRate().currency(), priced.fxRateSnapshot().rate());
        activityLineItemRepository.save(lineItem);

        events.publishEvent(new ActivityLineItemAddedEvent(lineItemId, itineraryId, itinerary.getConsultantId(),
            priced.sellRate()));
        return lineItemId;
    }

    @Override
    @Transactional
    public void updateActivityHeadcount(UUID itineraryId, UUID lineItemId, int newHeadcount) {
        // Same DRAFT-only immutability boundary as adding a line item —
        // see ActivityLineItem.updateHeadcount's Javadoc for the scoping
        // note on what "post-confirmation" means in this codebase today.
        requireOwnedDraftItinerary(itineraryId);

        ActivityLineItem lineItem = activityLineItemRepository.findById(lineItemId)
            .orElseThrow(() -> new IllegalArgumentException("No activity line item: " + lineItemId));
        if (!lineItem.getItineraryId().equals(itineraryId)) {
            throw new IllegalArgumentException("Line item " + lineItemId + " does not belong to itinerary " + itineraryId);
        }
        lineItem.updateHeadcount(newHeadcount);
        activityLineItemRepository.save(lineItem);
    }

    @Override
    public Money consolidateCheckoutTotal(ConsolidateCheckoutTotalCommand command) {
        UUID itineraryId = command.itineraryId();
        CurrencyCode target = command.targetSellCurrency();

        List<Money> lineItemSellAmounts = new java.util.ArrayList<>();
        hotelLineItemRepository.findByItineraryId(itineraryId)
            .forEach(li -> lineItemSellAmounts.add(new Money(li.getSellRate(), li.getSellCurrency())));
        flightLineItemRepository.findByItineraryId(itineraryId)
            .forEach(li -> lineItemSellAmounts.add(new Money(li.getSellRate(), li.getSellCurrency())));
        transferLineItemRepository.findByItineraryId(itineraryId)
            .forEach(li -> lineItemSellAmounts.add(new Money(li.getSellRate(), li.getSellCurrency())));
        cruiseLineItemRepository.findByItineraryId(itineraryId)
            .forEach(li -> lineItemSellAmounts.add(new Money(li.getSellRate(), li.getSellCurrency())));
        activityLineItemRepository.findByItineraryId(itineraryId)
            .forEach(li -> lineItemSellAmounts.add(new Money(li.getSellRate(), li.getSellCurrency())));

        Money total = Money.zero(target);
        for (Money amount : lineItemSellAmounts) {
            if (amount.currency() == target) {
                total = total.plus(amount);
                continue;
            }
            // PRD §23.1 Edge Case #2: a line item in a currency other than
            // the target must never be silently added as-is (that's
            // exactly the mixed-currency total this story exists to
            // prevent) — a missing rate is a hard failure, not a fallback.
            BigDecimal rate = command.ratesToTargetCurrency().get(amount.currency());
            if (rate == null) {
                throw new IllegalArgumentException(
                    "No conversion rate supplied for " + amount.currency() + " -> " + target
                        + "; cannot consolidate itinerary " + itineraryId + " to a single-currency total");
            }
            total = total.plus(amount.convertTo(target, rate));
        }
        return total;
    }

    @Override
    public RefundCalculation calculateCancellationRefund(UUID bookingId, CalculateCancellationRefundCommand command) {
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new IllegalArgumentException("No booking: " + bookingId));
        CurrentPrincipal.resolveTenantScope(booking.getConsultantId());
        requireActiveUnlessSuperAdmin(booking.getConsultantId());

        return paymentsApi.calculateRefund(new CalculateRefundCommand(bookingId, booking.getConsultantId(),
            command.sellPrice(), command.cancellationDeadline(), command.cancelledAt(),
            command.postDeadlinePenaltyPercent(), command.originalFxRateSnapshot()));
    }

    @Override
    @Transactional
    public CancellationRequestView submitCancellation(UUID bookingId, CalculateCancellationRefundCommand command) {
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new IllegalArgumentException("No booking: " + bookingId));
        CurrentPrincipal.resolveTenantScope(booking.getConsultantId());
        requireActiveUnlessSuperAdmin(booking.getConsultantId());

        RefundCalculation calculation = paymentsApi.calculateRefund(new CalculateRefundCommand(bookingId,
            booking.getConsultantId(), command.sellPrice(), command.cancellationDeadline(), command.cancelledAt(),
            command.postDeadlinePenaltyPercent(), command.originalFxRateSnapshot()));

        CancellationRequest request = CancellationRequest.submit(UUID.randomUUID(), bookingId, booking.getConsultantId(),
            calculation.refundAmount().amount(), calculation.refundAmount().currency(),
            calculation.penaltyAmount().amount(), calculation.penaltyAmount().currency(),
            calculation.requiresConsultantApproval());

        if (!calculation.requiresConsultantApproval()) {
            // FIN-16 AC: a penalty-free cancellation completes without an
            // explicit approval step — process the refund and cancel the
            // booking in this same transaction.
            paymentsApi.processRefund(
                new WalletHoldCommand(bookingId, booking.getConsultantId(), calculation.refundAmount()));
            request.markRefunded();
            booking.markCancelled();
            bookingRepository.save(booking);
            events.publishEvent(new BookingCancelledEvent(bookingId, booking.getConsultantId(),
                calculation.refundAmount(), calculation.penaltyAmount()));
        }

        cancellationRequestRepository.save(request);
        return toCancellationRequestView(request);
    }

    @Override
    @Transactional
    public CancellationRequestView approveCancellation(UUID cancellationRequestId) {
        CancellationRequest request = cancellationRequestRepository.findById(cancellationRequestId)
            .orElseThrow(() -> new IllegalArgumentException("No cancellation request: " + cancellationRequestId));
        CurrentPrincipal.resolveTenantScope(request.getConsultantId());
        requireActiveUnlessSuperAdmin(request.getConsultantId());

        // PRD §12.5 AC: a penalized cancellation pauses HERE until a
        // Consultant explicitly approves — approve() throws if this
        // request isn't PENDING_APPROVAL (already approved/refunded, or
        // never required approval in the first place).
        request.approve();

        Money refundAmount = new Money(request.getRefundAmount(), request.getRefundCurrency());
        Money penaltyAmount = new Money(request.getPenaltyAmount(), request.getPenaltyCurrency());
        paymentsApi.processRefund(new WalletHoldCommand(request.getBookingId(), request.getConsultantId(), refundAmount));
        request.markRefunded();
        cancellationRequestRepository.save(request);

        Booking booking = bookingRepository.findById(request.getBookingId())
            .orElseThrow(() -> new IllegalArgumentException("No booking: " + request.getBookingId()));
        booking.markCancelled();
        bookingRepository.save(booking);

        events.publishEvent(new BookingCancelledEvent(request.getBookingId(), request.getConsultantId(),
            refundAmount, penaltyAmount));

        return toCancellationRequestView(request);
    }

    @Override
    @Transactional
    public DisputeTicketView flagDispute(UUID bookingId, FlagDisputeCommand command) {
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new IllegalArgumentException("No booking: " + bookingId));
        CurrentPrincipal.resolveTenantScope(booking.getConsultantId());
        requireActiveUnlessSuperAdmin(booking.getConsultantId());

        booking.markDisputed();
        bookingRepository.save(booking);

        UUID disputeTicketId = UUID.randomUUID();
        DisputeTicket ticket = new DisputeTicket(disputeTicketId, bookingId, booking.getConsultantId(), command.reason());
        disputeTicketRepository.save(ticket);

        events.publishEvent(
            new DisputeTicketCreatedEvent(disputeTicketId, bookingId, booking.getConsultantId(), command.reason()));

        return new DisputeTicketView(disputeTicketId, bookingId, ticket.getReason(), ticket.getStatus().name(),
            ticket.getCreatedAt());
    }

    @Override
    public Page<DisputeTicketView> findDisputeTickets(UUID consultantId, Pageable pageable) {
        UUID scopedConsultantId = CurrentPrincipal.resolveTenantScope(consultantId);
        return disputeTicketRepository.findByConsultantId(scopedConsultantId, pageable)
            .map(ticket -> new DisputeTicketView(ticket.getDisputeTicketId(), ticket.getBookingId(),
                ticket.getReason(), ticket.getStatus().name(), ticket.getCreatedAt()));
    }

    @Override
    public Page<BookingSearchResultView> searchByPnrReference(String pnrReference, Pageable pageable) {
        List<BookingSearchResultView> results = bookingRepository.findByPnrSearchableRef(pnrReference)
            .map(booking -> {
                CurrentPrincipal.resolveTenantScope(booking.getConsultantId());
                return new BookingSearchResultView(booking.getBookingId(), booking.getPnrSearchableRef(),
                    booking.getStatus().name(), new Money(booking.getTotalSellPriceAmount(), booking.getTotalSellCurrency()),
                    booking.getPaymentMethod().name(), booking.getCreatedAt());
            })
            .map(List::of)
            .orElseGet(List::of);
        return new org.springframework.data.domain.PageImpl<>(results, pageable, results.size());
    }

    private static CancellationRequestView toCancellationRequestView(CancellationRequest request) {
        return new CancellationRequestView(request.getCancellationRequestId(), request.getBookingId(),
            new Money(request.getRefundAmount(), request.getRefundCurrency()),
            new Money(request.getPenaltyAmount(), request.getPenaltyCurrency()),
            request.getStatus().name());
    }

    @Override
    @Transactional
    public void recalculateQuotation(UUID quotationId, int newTravelerCount) {
        Quotation quotation = quotationRepository.findById(quotationId)
            .orElseThrow(() -> new IllegalArgumentException("No quotation: " + quotationId));
        Itinerary itinerary = itineraryRepository.findById(quotation.getItineraryId())
            .orElseThrow(() -> new IllegalArgumentException("No itinerary: " + quotation.getItineraryId()));
        CurrentPrincipal.resolveTenantScope(itinerary.getConsultantId());
        requireActiveUnlessSuperAdmin(itinerary.getConsultantId());

        // PRD §23.1 Edge Case #3's "after Quotation but before booking"
        // boundary — BOK-16's real Itinerary.status transition to BOOKED
        // is the authoritative signal here, not Quotation's own
        // convertedToBookingId field (nothing in this codebase sets it).
        if (itinerary.getStatus() == ItineraryStatus.BOOKED) {
            throw new IllegalStateException(
                "Cannot recalculate quotation " + quotationId + ": itinerary is already BOOKED");
        }

        quotation.recalculate(newTravelerCount, Instant.now().plus(QUOTATION_VALIDITY_WINDOW));
        quotationRepository.save(quotation);

        events.publishEvent(new QuotationRecalculatedEvent(
            quotationId, quotation.getItineraryId(), itinerary.getConsultantId(), newTravelerCount));
    }

    // PRD §22.3 T4 / BOK-08: once saved as a Quotation, an itinerary is
    // read-only "except via explicit edit" — adding a line item here is an
    // implicit edit, not that explicit path (BOK-18's traveler-count
    // recalculation is the first such explicit-edit mechanism), so it's
    // blocked once the itinerary has left DRAFT. Shared by every
    // add*LineItem method (BOK-03 through BOK-07).
    private Itinerary requireOwnedDraftItinerary(UUID itineraryId) {
        Itinerary itinerary = itineraryRepository.findById(itineraryId)
            .orElseThrow(() -> new IllegalArgumentException("No itinerary: " + itineraryId));
        CurrentPrincipal.resolveTenantScope(itinerary.getConsultantId());
        requireActiveUnlessSuperAdmin(itinerary.getConsultantId());

        if (itinerary.getStatus() != ItineraryStatus.DRAFT) {
            throw new IllegalStateException(
                "Cannot add a line item: itinerary " + itineraryId + " is " + itinerary.getStatus() + ", not DRAFT");
        }
        return itinerary;
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

    @Override
    public BookingView findBookingById(UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new IllegalArgumentException("No such booking: " + bookingId));
        CurrentPrincipal.resolveTenantScope(booking.getConsultantId());

        // BOK-15: every CONFIRMED booking gets a Voucher generated
        // synchronously in the SAME transaction as confirmation itself
        // (finalizeConfirmedBooking) — a missing one here would be a data
        // integrity bug, not a normal "not yet generated" state to hide behind Optional.
        Voucher voucher = voucherRepository.findByBookingId(bookingId)
            .orElseThrow(() -> new IllegalStateException("No voucher for confirmed booking: " + bookingId));

        return new BookingView(
            booking.getBookingId(),
            booking.getPnrSearchableRef(),
            booking.getStatus().name(),
            booking.getPaymentMethod().name(),
            new Money(booking.getTotalSellPriceAmount(), booking.getTotalSellCurrency()),
            booking.getCreatedAt(),
            new VoucherView(voucher.getPdfReference(), voucher.getAtolCertificateReference(), voucher.getGeneratedAt()));
    }

    @Override
    public byte[] downloadVoucherPdf(UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new IllegalArgumentException("No such booking: " + bookingId));
        CurrentPrincipal.resolveTenantScope(booking.getConsultantId());
        return voucherService.retrievePdf(bookingId);
    }

    @Override
    @Transactional
    public UUID convertQuotationToPackage(UUID quotationId, ConvertQuotationToPackageCommand command) {
        Quotation quotation = quotationRepository.findById(quotationId)
            .orElseThrow(() -> new IllegalArgumentException("No quotation: " + quotationId));
        Itinerary itinerary = itineraryRepository.findById(quotation.getItineraryId())
            .orElseThrow(() -> new IllegalArgumentException("No itinerary: " + quotation.getItineraryId()));
        CurrentPrincipal.resolveTenantScope(itinerary.getConsultantId());
        requireActiveUnlessSuperAdmin(itinerary.getConsultantId());

        if (command.validityEnd().isBefore(command.validityStart())) {
            throw new IllegalArgumentException("validityEnd must not be before validityStart");
        }
        if (command.maxPax() <= 0) {
            throw new IllegalArgumentException("maxPax must be a positive value");
        }

        // FIN-05: basePrice is auto-filled from the source itinerary's
        // already-priced line items — BOK-17's mixed-currency consolidation
        // is a later, separate story, so this assumes every line item
        // shares one sell currency (true for this vertical slice's
        // Hotel-only itineraries).
        List<HotelLineItem> lineItems = hotelLineItemRepository.findByItineraryId(quotation.getItineraryId());
        if (lineItems.isEmpty()) {
            throw new IllegalStateException(
                "Cannot convert to package: itinerary " + quotation.getItineraryId() + " has no line items");
        }
        Money basePrice = lineItems.stream()
            .map(lineItem -> new Money(lineItem.getSellRate(), lineItem.getSellCurrency()))
            .reduce(Money::plus)
            .orElseThrow();

        UUID packageId = UUID.randomUUID();
        TravelPackage travelPackage = new TravelPackage(packageId, quotation.getItineraryId(),
            itinerary.getConsultantId(), command.name(), command.description(), command.validityStart(),
            command.validityEnd(), basePrice.amount(), command.markupPrice(), basePrice.currency(), command.maxPax());

        // BOK-11, PRD §20.7 is_dynamic_flight_hotel_combo: now that BOK-04
        // (Flight line items) exists, this is detectable — a package is a
        // "dynamic" flight+hotel combo when its source itinerary carries
        // both product types.
        boolean hasHotel = !lineItems.isEmpty();
        boolean hasFlight = !flightLineItemRepository.findByItineraryId(quotation.getItineraryId()).isEmpty();
        if (hasHotel && hasFlight) {
            travelPackage.markDynamicFlightHotelCombo();
        }

        travelPackageRepository.save(travelPackage);

        events.publishEvent(new PackageCreatedEvent(packageId, quotation.getItineraryId(), itinerary.getConsultantId()));
        return packageId;
    }

    @Override
    @Transactional
    public UUID publishPackage(UUID packageId, boolean promoteViaAds) {
        TravelPackage travelPackage = travelPackageRepository.findById(packageId)
            .orElseThrow(() -> new IllegalArgumentException("No package: " + packageId));
        CurrentPrincipal.resolveTenantScope(travelPackage.getConsultantId());
        requireActiveUnlessSuperAdmin(travelPackage.getConsultantId());

        // BOK-11, PRD §17.2/§22.3 T5: blocks publish until ATOL disclosure
        // is complete, but only for a UK Consultant's dynamic combo —
        // requireAtolDisclosureIfNeeded is a no-op for every other case.
        Market market = whitelabelApi.findConsultantMarket(travelPackage.getConsultantId());
        travelPackage.requireAtolDisclosureIfNeeded(market == Market.UK);

        travelPackage.publish(promoteViaAds);
        travelPackageRepository.save(travelPackage);

        events.publishEvent(new PackagePublishedEvent(packageId, travelPackage.getSourceItineraryId(),
            travelPackage.getConsultantId(), promoteViaAds));
        return packageId;
    }

    @Override
    @Transactional
    public void completeAtolDisclosure(UUID packageId) {
        TravelPackage travelPackage = travelPackageRepository.findById(packageId)
            .orElseThrow(() -> new IllegalArgumentException("No package: " + packageId));
        CurrentPrincipal.resolveTenantScope(travelPackage.getConsultantId());
        requireActiveUnlessSuperAdmin(travelPackage.getConsultantId());

        travelPackage.completeAtolDisclosure();
        travelPackageRepository.save(travelPackage);
    }

    @Override
    @Transactional
    public void updatePackagePrice(UUID packageId, BigDecimal newMarkupPrice) {
        TravelPackage travelPackage = travelPackageRepository.findById(packageId)
            .orElseThrow(() -> new IllegalArgumentException("No package: " + packageId));
        CurrentPrincipal.resolveTenantScope(travelPackage.getConsultantId());
        requireActiveUnlessSuperAdmin(travelPackage.getConsultantId());

        travelPackage.updateMarkupPrice(newMarkupPrice);
        travelPackageRepository.save(travelPackage);

        events.publishEvent(new PackagePriceChangedEvent(packageId, travelPackage.getConsultantId()));
    }

    @Override
    public Page<PackageView> findPublishedPackagesByConsultant(UUID consultantId, Pageable pageable) {
        UUID scopedConsultantId = CurrentPrincipal.resolveTenantScope(consultantId);
        return travelPackageRepository.findByConsultantIdAndStatus(scopedConsultantId, PackageStatus.PUBLISHED, pageable)
            .map(BookingServiceImpl::toPackageView);
    }

    @Override
    public PackageView findPackageById(UUID packageId) {
        TravelPackage travelPackage = travelPackageRepository.findById(packageId)
            .orElseThrow(() -> new IllegalArgumentException("No package: " + packageId));
        CurrentPrincipal.resolveTenantScope(travelPackage.getConsultantId());
        if (travelPackage.getStatus() != PackageStatus.PUBLISHED) {
            throw new IllegalStateException("Package " + packageId + " is not published");
        }
        return toPackageView(travelPackage);
    }

    private static PackageView toPackageView(TravelPackage travelPackage) {
        return new PackageView(travelPackage.getPackageId(), travelPackage.getSourceItineraryId(),
            travelPackage.getConsultantId(), travelPackage.getName(), travelPackage.getDescription(),
            travelPackage.getValidityStart(), travelPackage.getValidityEnd(), travelPackage.getBasePrice(),
            travelPackage.getMarkupPrice(), travelPackage.getCurrency(), travelPackage.getMaxPax(),
            travelPackage.isPromotedViaAds());
    }

    @Override
    public ConsultantBookingMetricsView findConsultantBookingMetrics(UUID consultantId) {
        UUID scopedConsultantId = CurrentPrincipal.resolveTenantScope(consultantId);
        Instant monthStart = LocalDate.now(ZoneOffset.UTC).withDayOfMonth(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        List<Booking> bookingsThisMonth =
            bookingRepository.findByConsultantIdAndCreatedAtGreaterThanEqual(scopedConsultantId, monthStart);

        // Same "no data yet, default to INR" simplification PaymentsServiceImpl#provisionWallet
        // already established for a brand-new Consultant with nothing to report.
        Money gmvThisMonth = bookingsThisMonth.stream()
            .map(b -> new Money(b.getTotalSellPriceAmount(), b.getTotalSellCurrency()))
            .reduce(Money::plus)
            .orElseGet(() -> Money.zero(CurrencyCode.INR));

        return new ConsultantBookingMetricsView(bookingsThisMonth.size(), gmvThisMonth);
    }

    @Override
    public List<PackageSummaryView> findTopPackagesForConsultant(UUID consultantId, int limit) {
        UUID scopedConsultantId = CurrentPrincipal.resolveTenantScope(consultantId);

        // Bounded by "how many packages/bookings one Consultant realistically
        // has" (MVP scale) — a single pass over each collection, grouped
        // in memory, avoids N+1 per-package count queries (backend-best-
        // practices §5).
        Map<UUID, Long> bookingCountBySourceItineraryId = bookingRepository.findByConsultantId(scopedConsultantId)
            .stream()
            .collect(Collectors.groupingBy(Booking::getItineraryId, Collectors.counting()));

        return travelPackageRepository.findByConsultantIdAndStatus(
                scopedConsultantId, PackageStatus.PUBLISHED, Pageable.unpaged())
            .stream()
            .map(pkg -> new PackageSummaryView(pkg.getPackageId(), pkg.getName(),
                bookingCountBySourceItineraryId.getOrDefault(pkg.getSourceItineraryId(), 0L)))
            .sorted(Comparator.comparingLong(PackageSummaryView::bookingCount).reversed())
            .limit(limit)
            .toList();
    }

    @Override
    public Page<QuotationSummaryView> findPendingQuotationsForConsultant(UUID consultantId, Pageable pageable) {
        UUID scopedConsultantId = CurrentPrincipal.resolveTenantScope(consultantId);
        return itineraryRepository.findByConsultantIdAndStatus(scopedConsultantId, ItineraryStatus.QUOTATION, pageable)
            .map(itinerary -> new QuotationSummaryView(itinerary.getItineraryId(), itinerary.getCreatedAt()));
    }

    @Override
    public AllConsultantGmvView findAllConsultantGmv() {
        List<CurrencyAmount> gmvByCurrency = bookingRepository.sumTotalSellPriceGroupedByCurrency().stream()
            .map(row -> new CurrencyAmount((CurrencyCode) row[0], (BigDecimal) row[1]))
            .toList();
        return new AllConsultantGmvView(gmvByCurrency);
    }

    @Override
    public List<SupplierPerformanceView> findSupplierPerformanceSummary() {
        return Arrays.stream(SupplierId.values())
            .map(supplierId -> new SupplierPerformanceView(supplierId,
                hotelLineItemRepository.countBySupplierId(supplierId)
                    + flightLineItemRepository.countBySupplierId(supplierId)
                    + transferLineItemRepository.countBySupplierId(supplierId)
                    + cruiseLineItemRepository.countBySupplierId(supplierId)
                    + activityLineItemRepository.countBySupplierId(supplierId)))
            .toList();
    }
}
