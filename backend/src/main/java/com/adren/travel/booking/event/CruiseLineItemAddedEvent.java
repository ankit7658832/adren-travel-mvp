package com.adren.travel.booking.event;

import com.adren.travel.shared.Money;

import java.util.UUID;

/** Published when a Cruise line item is added to an itinerary (PRD §20.5, BOK-06). */
public record CruiseLineItemAddedEvent(UUID lineItemId, UUID itineraryId, UUID consultantId, Money sellRate) {
}
