package com.adren.travel.booking.event;

import com.adren.travel.shared.Money;

import java.util.UUID;

/** Published when a Hotel line item is added to an itinerary (PRD §20.2, BOK-03). */
public record HotelLineItemAddedEvent(UUID lineItemId, UUID itineraryId, UUID consultantId, Money sellRate) {
}
