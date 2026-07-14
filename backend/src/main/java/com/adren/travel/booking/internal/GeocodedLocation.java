package com.adren.travel.booking.internal;

/**
 * One geocoded, inventory-checked location — PRD §9.1 Flow A / §21.1's
 * "every location gets a pin, even one with no inventory" (T1) requirement
 * as a first-class field rather than something the frontend has to infer.
 */
record GeocodedLocation(String locationCode, String displayName, double latitude, double longitude,
                         boolean hasInventory) {
}
