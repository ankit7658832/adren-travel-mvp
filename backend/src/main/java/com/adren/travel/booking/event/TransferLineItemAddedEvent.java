package com.adren.travel.booking.event;

import com.adren.travel.shared.Money;

import java.util.UUID;

/** Published when a Transfer line item is added to an itinerary (PRD §20.4, BOK-05). */
public record TransferLineItemAddedEvent(UUID lineItemId, UUID itineraryId, UUID consultantId, Money sellRate) {
}
