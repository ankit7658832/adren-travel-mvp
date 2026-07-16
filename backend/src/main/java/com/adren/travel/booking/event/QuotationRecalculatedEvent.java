package com.adren.travel.booking.event;

import java.util.UUID;

/** Published when a Quotation's traveler count changes (PRD §23.1 Edge Case #3, BOK-18). */
public record QuotationRecalculatedEvent(UUID quotationId, UUID itineraryId, UUID consultantId, int newTravelerCount) {
}
