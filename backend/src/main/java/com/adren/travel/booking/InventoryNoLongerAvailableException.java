package com.adren.travel.booking;

import java.util.UUID;

/**
 * Thrown when a second concurrent confirmation attempt for the same
 * itinerary loses the optimistic-locking race (PRD §23.1 Edge Case #1,
 * BOK-16) — mapped to a 409 "no longer available" response, not treated as
 * an unexpected/500-class failure, since losing this race is a normal,
 * anticipated outcome of concurrent booking attempts.
 */
public class InventoryNoLongerAvailableException extends RuntimeException {

    public InventoryNoLongerAvailableException(UUID itineraryId) {
        super("This itinerary is no longer available to book — it was just confirmed by another request: " + itineraryId);
    }
}
