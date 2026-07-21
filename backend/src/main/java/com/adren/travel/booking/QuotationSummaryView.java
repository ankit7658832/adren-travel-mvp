package com.adren.travel.booking;

import java.time.Instant;
import java.util.UUID;

/** HRD-09, PRD §9.5/§21.5 — the Consultant Dashboard's Pending Quotations tab: an itinerary still at QUOTATION, not yet booked. */
public record QuotationSummaryView(UUID itineraryId, Instant createdAt) {
}
