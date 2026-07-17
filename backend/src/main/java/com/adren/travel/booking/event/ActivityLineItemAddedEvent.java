package com.adren.travel.booking.event;

import com.adren.travel.shared.Money;

import java.util.UUID;

/** Published when an Activity line item is added to an itinerary (PRD §20.6, BOK-07). */
public record ActivityLineItemAddedEvent(UUID lineItemId, UUID itineraryId, UUID consultantId, Money sellRate) {
}
