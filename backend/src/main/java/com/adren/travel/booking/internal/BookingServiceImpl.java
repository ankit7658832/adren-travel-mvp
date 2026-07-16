package com.adren.travel.booking.internal;

import com.adren.travel.booking.AddCruiseLineItemCommand;
import com.adren.travel.booking.AddFlightLineItemCommand;
import com.adren.travel.booking.AddHotelLineItemCommand;
import com.adren.travel.booking.AddTransferLineItemCommand;
import com.adren.travel.booking.AlternateOption;
import com.adren.travel.booking.BookingApi;
import com.adren.travel.booking.ConvertQuotationToPackageCommand;
import com.adren.travel.booking.CreateTravelerProfileCommand;
import com.adren.travel.booking.PackageView;
import com.adren.travel.booking.event.BookingConfirmedEvent;
import com.adren.travel.booking.event.CruiseLineItemAddedEvent;
import com.adren.travel.booking.event.FlightLineItemAddedEvent;
import com.adren.travel.booking.event.HotelLineItemAddedEvent;
import com.adren.travel.booking.event.ItineraryQuotationSavedEvent;
import com.adren.travel.booking.event.TransferLineItemAddedEvent;
import com.adren.travel.booking.event.PackageCreatedEvent;
import com.adren.travel.booking.event.PackagePublishedEvent;
import com.adren.travel.booking.event.TravelerProfileCreatedEvent;
import com.adren.travel.payments.CalculateSellRateCommand;
import com.adren.travel.payments.PaymentsApi;
import com.adren.travel.payments.SellRateCalculation;
import com.adren.travel.payments.WalletHoldCommand;
import com.adren.travel.security.CurrentPrincipal;
import com.adren.travel.shared.Money;
import com.adren.travel.shared.ProductCategory;
import com.adren.travel.supplier.SupplierSearchApi;
import com.adren.travel.whitelabel.WhitelabelApi;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
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
    private final QuotationRepository quotationRepository;
    private final TravelPackageRepository travelPackageRepository;
    private final VoucherService voucherService;
    private final ApplicationEventPublisher events;
    private final WhitelabelApi whitelabelApi;
    private final SupplierSearchApi supplierSearchApi;
    private final PaymentsApi paymentsApi;

    BookingServiceImpl(ItineraryRepository itineraryRepository, TravelerProfileRepository travelerProfileRepository,
                        HotelLineItemRepository hotelLineItemRepository, FlightLineItemRepository flightLineItemRepository,
                        TransferLineItemRepository transferLineItemRepository,
                        CruiseLineItemRepository cruiseLineItemRepository,
                        QuotationRepository quotationRepository,
                        TravelPackageRepository travelPackageRepository, VoucherService voucherService,
                        ApplicationEventPublisher events, WhitelabelApi whitelabelApi,
                        SupplierSearchApi supplierSearchApi, PaymentsApi paymentsApi) {
        this.itineraryRepository = itineraryRepository;
        this.travelerProfileRepository = travelerProfileRepository;
        this.hotelLineItemRepository = hotelLineItemRepository;
        this.flightLineItemRepository = flightLineItemRepository;
        this.transferLineItemRepository = transferLineItemRepository;
        this.cruiseLineItemRepository = cruiseLineItemRepository;
        this.quotationRepository = quotationRepository;
        this.travelPackageRepository = travelPackageRepository;
        this.voucherService = voucherService;
        this.events = events;
        this.whitelabelApi = whitelabelApi;
        this.supplierSearchApi = supplierSearchApi;
        this.paymentsApi = paymentsApi;
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

        // BOK-08 AC: "at least one line item per required category" — this
        // vertical slice only implements Hotel line items (BOK-04..07's
        // other categories aren't built yet), so that narrows to "at least
        // one line item" given the categories this slice actually supports.
        if (hotelLineItemRepository.findByItineraryId(itineraryId).isEmpty()) {
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
    public UUID confirmBooking(UUID quotationOrPackageId, Money totalSellPrice) {
        UUID consultantId = resolveConsultantIdFor(quotationOrPackageId);
        CurrentPrincipal.resolveTenantScope(consultantId);
        requireActiveUnlessSuperAdmin(consultantId);

        UUID bookingId = UUID.randomUUID(); // simplified — a real Booking entity would be created/persisted here

        // FIN-07: this direct (non-Stripe) confirmBooking path is the
        // wallet/on-account payment method — Stripe payments instead go
        // through confirmBookingFromPaymentWebhook and never touch the
        // wallet. Placing then immediately resolving the hold in the same
        // call is a simplification: this scaffold has no separate "reach
        // payment step" moment distinct from confirmation itself, unlike
        // the full booking flow PRD §12.3 describes.
        paymentsApi.placeHold(new WalletHoldCommand(bookingId, consultantId, totalSellPrice));
        paymentsApi.resolveHoldAsDebit(new WalletHoldCommand(bookingId, consultantId, totalSellPrice));

        return finalizeConfirmedBooking(bookingId, consultantId, totalSellPrice);
    }

    // BOK-13 — quotationOrPackageId is polymorphic: a booking can be
    // confirmed directly from a Quotation or from a published Package.
    private UUID resolveConsultantIdFor(UUID quotationOrPackageId) {
        return quotationRepository.findById(quotationOrPackageId)
            .map(quotation -> itineraryRepository.findById(quotation.getItineraryId())
                .orElseThrow(() -> new IllegalArgumentException("No itinerary: " + quotation.getItineraryId()))
                .getConsultantId())
            .or(() -> travelPackageRepository.findById(quotationOrPackageId).map(TravelPackage::getConsultantId))
            .orElseThrow(() -> new IllegalArgumentException("No quotation or package: " + quotationOrPackageId));
    }

    @Override
    @Transactional
    public UUID confirmBookingFromPaymentWebhook(UUID quotationOrPackageId, UUID consultantId, Money totalSellPrice) {
        // FIN-07: this Stripe path never touches the wallet — the customer
        // already paid by card, not wallet/credit.
        UUID bookingId = UUID.randomUUID(); // simplified — a real Booking entity would be created/persisted here
        return finalizeConfirmedBooking(bookingId, consultantId, totalSellPrice);
    }

    private UUID finalizeConfirmedBooking(UUID bookingId, UUID consultantId, Money totalSellPrice) {
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
        return supplierSearchApi.searchHotels(locationCode, checkIn, checkOut).stream()
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

        travelPackage.publish(promoteViaAds);
        travelPackageRepository.save(travelPackage);

        events.publishEvent(new PackagePublishedEvent(packageId, travelPackage.getSourceItineraryId(),
            travelPackage.getConsultantId(), promoteViaAds));
        return packageId;
    }

    @Override
    public Page<PackageView> findPublishedPackagesByConsultant(UUID consultantId, Pageable pageable) {
        UUID scopedConsultantId = CurrentPrincipal.resolveTenantScope(consultantId);
        return travelPackageRepository.findByConsultantIdAndStatus(scopedConsultantId, PackageStatus.PUBLISHED, pageable)
            .map(BookingServiceImpl::toPackageView);
    }

    private static PackageView toPackageView(TravelPackage travelPackage) {
        return new PackageView(travelPackage.getPackageId(), travelPackage.getSourceItineraryId(),
            travelPackage.getName(), travelPackage.getDescription(), travelPackage.getValidityStart(),
            travelPackage.getValidityEnd(), travelPackage.getBasePrice(), travelPackage.getMarkupPrice(),
            travelPackage.getCurrency(), travelPackage.getMaxPax(), travelPackage.isPromotedViaAds());
    }
}
