package com.adren.travel.booking.event;

import com.adren.travel.shared.Money;

import java.util.UUID;

/** Published when a Flight line item is added to an itinerary (PRD §20.3, BOK-04). */
public record FlightLineItemAddedEvent(UUID lineItemId, UUID itineraryId, UUID consultantId, Money sellRate) {
}
