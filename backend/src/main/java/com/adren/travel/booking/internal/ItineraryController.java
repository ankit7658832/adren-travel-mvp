package com.adren.travel.booking.internal;

import com.adren.travel.ai.AiItineraryGenerationResult;
import com.adren.travel.booking.AddActivityLineItemCommand;
import com.adren.travel.booking.AddCruiseLineItemCommand;
import com.adren.travel.booking.AddFlightLineItemCommand;
import com.adren.travel.booking.AddHotelLineItemCommand;
import com.adren.travel.booking.AddTransferLineItemCommand;
import com.adren.travel.booking.AlternateOption;
import com.adren.travel.booking.BookingApi;
import com.adren.travel.booking.ConsolidateCheckoutTotalCommand;
import com.adren.travel.booking.GenerateAiSuggestionCommand;
import com.adren.travel.shared.Money;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * HTTP surface for the Booking module (consumed by the React+Vite frontend).
 * Controllers stay thin — all logic lives in {@link BookingServiceImpl},
 * reached here only through the public {@link BookingApi}.
 */
@RestController
@RequestMapping("/api/v1/itineraries")
class ItineraryController {

    private final BookingApi bookingApi;

    ItineraryController(BookingApi bookingApi) {
        this.bookingApi = bookingApi;
    }

    @PostMapping("/{itineraryId}/quotation")
    UUID saveAsQuotation(@PathVariable UUID itineraryId) {
        return bookingApi.saveAsQuotation(itineraryId);
    }

    /** PRD §11.1/§11.2, AI-02 — "Complete with AI" entry point's backend: a grounded suggestion or an explicit no-viable-suggestion outcome. */
    @PostMapping("/{itineraryId}/ai-suggestion")
    AiItineraryGenerationResult generateAiItinerarySuggestion(@PathVariable UUID itineraryId,
                                                               @Valid @RequestBody GenerateAiSuggestionRequest request) {
        Money budgetLimit = request.budgetAmount() != null ? new Money(request.budgetAmount(), request.budgetCurrency()) : null;
        return bookingApi.generateAiItinerarySuggestion(itineraryId, new GenerateAiSuggestionCommand(
            request.locationCode(), request.checkIn(), request.checkOut(), request.naturalLanguageRequest(), budgetLimit));
    }

    /**
     * The Itinerary Builder's alternate-selection side panel (PRD §21.2,
     * FND-16) — {@code checkIn}/{@code checkOut} default the same way
     * {@code SearchController} does when the Consultant hasn't picked
     * dates yet.
     */
    @GetMapping("/{itineraryId}/alternates")
    List<AlternateOption> findAlternates(
        @PathVariable UUID itineraryId,
        @RequestParam String location,
        @RequestParam(required = false) String category,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut) {
        LocalDate resolvedCheckIn = checkIn != null ? checkIn : LocalDate.now().plusDays(30);
        LocalDate resolvedCheckOut = checkOut != null ? checkOut : resolvedCheckIn.plusDays(3);
        return bookingApi.findAlternates(itineraryId, location, category, resolvedCheckIn, resolvedCheckOut);
    }

    /** PRD §20.2, §9.3 — adds a Hotel line item to the itinerary (BOK-03). */
    @PostMapping("/{itineraryId}/line-items/hotel")
    @ResponseStatus(HttpStatus.CREATED)
    Map<String, UUID> addHotelLineItem(@PathVariable UUID itineraryId, @Valid @RequestBody AddHotelLineItemRequest request) {
        UUID lineItemId = bookingApi.addHotelLineItem(itineraryId, new AddHotelLineItemCommand(
            request.supplierId(), request.supplierRateId(), request.propertyName(), request.roomType(),
            request.mealPlan(), request.cancellationDeadline(),
            new Money(request.netRate(), request.netRateCurrency()), request.sellCurrency(),
            request.fxRate(), request.bufferPercent(), request.commissionPercent()));
        return Map.of("lineItemId", lineItemId);
    }

    /** PRD §20.3, §10.2.4 — adds a Flight line item to the itinerary (BOK-04). */
    @PostMapping("/{itineraryId}/line-items/flight")
    @ResponseStatus(HttpStatus.CREATED)
    Map<String, UUID> addFlightLineItem(@PathVariable UUID itineraryId, @Valid @RequestBody AddFlightLineItemRequest request) {
        UUID lineItemId = bookingApi.addFlightLineItem(itineraryId, new AddFlightLineItemCommand(
            request.supplierId(), request.supplierRateId(), request.airlineCode(), request.flightNumber(),
            request.cabinClass(), request.baggageAllowance(),
            new Money(request.netRate(), request.netRateCurrency()), request.sellCurrency(),
            request.fxRate(), request.bufferPercent(), request.commissionPercent()));
        return Map.of("lineItemId", lineItemId);
    }

    /** PRD §20.4, §10.2.5 — adds a Transfer line item to the itinerary (BOK-05). */
    @PostMapping("/{itineraryId}/line-items/transfer")
    @ResponseStatus(HttpStatus.CREATED)
    Map<String, UUID> addTransferLineItem(@PathVariable UUID itineraryId, @Valid @RequestBody AddTransferLineItemRequest request) {
        UUID lineItemId = bookingApi.addTransferLineItem(itineraryId, new AddTransferLineItemCommand(
            request.supplierId(), request.supplierRateId(), request.vehicleType(), request.pickupPoint(),
            request.dropoffPoint(), new Money(request.netRate(), request.netRateCurrency()), request.sellCurrency(),
            request.fxRate(), request.bufferPercent(), request.commissionPercent()));
        return Map.of("lineItemId", lineItemId);
    }

    /** PRD §20.5, §10.2.6 — adds a Cruise line item to the itinerary (BOK-06). */
    @PostMapping("/{itineraryId}/line-items/cruise")
    @ResponseStatus(HttpStatus.CREATED)
    Map<String, UUID> addCruiseLineItem(@PathVariable UUID itineraryId, @Valid @RequestBody AddCruiseLineItemRequest request) {
        UUID lineItemId = bookingApi.addCruiseLineItem(itineraryId, new AddCruiseLineItemCommand(
            request.supplierId(), request.supplierRateId(), request.cruiseLine(), request.cabinCategory(),
            request.ports(), request.passengerDocumentsRequired(),
            new Money(request.netRate(), request.netRateCurrency()), request.sellCurrency(),
            request.fxRate(), request.bufferPercent(), request.commissionPercent()));
        return Map.of("lineItemId", lineItemId);
    }

    /** PRD §20.6, §10.2.7 — adds an Activity line item to the itinerary (BOK-07). */
    @PostMapping("/{itineraryId}/line-items/activity")
    @ResponseStatus(HttpStatus.CREATED)
    Map<String, UUID> addActivityLineItem(@PathVariable UUID itineraryId, @Valid @RequestBody AddActivityLineItemRequest request) {
        UUID lineItemId = bookingApi.addActivityLineItem(itineraryId, new AddActivityLineItemCommand(
            request.supplierId(), request.supplierRateId(), request.durationMinutes(), request.timeSlot(),
            request.headcount(), new Money(request.netRate(), request.netRateCurrency()), request.sellCurrency(),
            request.fxRate(), request.bufferPercent(), request.commissionPercent()));
        return Map.of("lineItemId", lineItemId);
    }

    /**
     * PRD §10.2.7 — changes an Activity line item's headcount, blocked once
     * the itinerary has left DRAFT (BOK-07).
     */
    @PatchMapping("/{itineraryId}/line-items/activity/{lineItemId}/headcount")
    void updateActivityHeadcount(@PathVariable UUID itineraryId, @PathVariable UUID lineItemId,
                                  @RequestBody UpdateActivityHeadcountRequest request) {
        bookingApi.updateActivityHeadcount(itineraryId, lineItemId, request.headcount());
    }

    /**
     * PRD §23.1 Edge Case #2 — consolidates the itinerary's line items into
     * one total in the requested sell currency (BOK-17), for the checkout
     * screen to call before {@code POST .../bookings}.
     */
    @PostMapping("/{itineraryId}/checkout-total")
    Money consolidateCheckoutTotal(@PathVariable UUID itineraryId, @Valid @RequestBody ConsolidateCheckoutTotalRequest request) {
        return bookingApi.consolidateCheckoutTotal(new ConsolidateCheckoutTotalCommand(
            itineraryId, request.targetSellCurrency(), request.ratesToTargetCurrency()));
    }
}
