package com.adren.travel.booking.event;

import java.util.UUID;

/**
 * Published when an itinerary is saved as a Quotation
 * (PRD Section 9.1, Flow A). Consumed by, e.g., the AI module to close out
 * the audit log entry for an AI-assisted itinerary (PRD Section 11.2).
 */
public record ItineraryQuotationSavedEvent(UUID itineraryId, UUID quotationId, UUID consultantId) {
}
