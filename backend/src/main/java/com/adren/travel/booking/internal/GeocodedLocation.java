package com.adren.travel.booking.internal;

/**
 * One geocoded, inventory-checked location — PRD §9.1 Flow A / §21.1's
 * "every location gets a pin, even one with no inventory" (T1) requirement
 * as a first-class field rather than something the frontend has to infer.
 * <p>
 * {@code autoSelectedSupplierRateId} is FND-14's Default Selection
 * Algorithm's output for this location — null when there's no inventory.
 * FND-15's "Auto-selected: Best available match" badge and FND-16's
 * Itinerary Builder both key off this field.
 */
record GeocodedLocation(String locationCode, String displayName, double latitude, double longitude,
                         boolean hasInventory, String autoSelectedSupplierRateId) {
}
