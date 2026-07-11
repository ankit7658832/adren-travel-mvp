package com.adren.travel.supplier;

import java.util.List;

/**
 * Public API of the Supplier module. Normalizes results across all 9 sources
 * (PRD Section 10.1) into {@link SupplierSearchResult} so the Booking module
 * never needs to know which supplier a given result came from until it's
 * displayed (PRD Section 9.4 — duplicate/normalization handling).
 */
public interface SupplierSearchApi {

    List<SupplierSearchResult> searchHotels(String locationCode, java.time.LocalDate checkIn, java.time.LocalDate checkOut);
}
