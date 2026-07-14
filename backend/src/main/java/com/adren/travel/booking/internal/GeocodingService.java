package com.adren.travel.booking.internal;

import org.springframework.stereotype.Component;

/**
 * Turns a free-text location query into map coordinates for PRD §9.1 Flow
 * A's "system geocodes each location and displays a map with pins" step.
 * <p>
 * No geocoding provider is named anywhere in the PRD or the supplier
 * catalogue (verified — the only external services specified are the
 * booking/payment/AI ones) — this is a deterministic, dependency-free
 * placeholder (a stable hash of the query mapped onto real-world
 * coordinate ranges) that satisfies "every location gets a pin, including
 * one with no inventory" (T1) without requiring an API key this MVP was
 * never scoped to acquire. Flagged as a NEEDS CLARIFICATION item — swap
 * this for a real provider (Google Maps/Mapbox/OpenStreetMap Nominatim)
 * once product confirms which one, without changing {@link GeoPoint}'s
 * shape or this class's call sites.
 */
@Component
class GeocodingService {

    record GeoPoint(double latitude, double longitude) {
    }

    GeoPoint geocode(String locationQuery) {
        int hash = locationQuery.trim().toLowerCase().hashCode();
        double latitude = 8.0 + (Math.floorMod(hash, 2700) / 100.0); // ~8°N–35°N (India bounding box)
        double longitude = 68.0 + (Math.floorMod(hash / 2700, 2900) / 100.0); // ~68°E–97°E
        return new GeoPoint(latitude, longitude);
    }
}
