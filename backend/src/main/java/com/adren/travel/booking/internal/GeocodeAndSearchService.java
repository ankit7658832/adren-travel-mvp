package com.adren.travel.booking.internal;

import com.adren.travel.supplier.SupplierSearchApi;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * Backs PRD §9.1 Flow A steps 2-4: geocode every location the Consultant
 * typed, and check whether any supplier has inventory there — one location
 * with no inventory must still render its own pin (T1), so this never
 * throws or drops a location, it marks it {@code hasInventory: false}.
 */
@Service
class GeocodeAndSearchService {

    private final GeocodingService geocodingService;
    private final SupplierSearchApi supplierSearchApi;

    GeocodeAndSearchService(GeocodingService geocodingService, SupplierSearchApi supplierSearchApi) {
        this.geocodingService = geocodingService;
        this.supplierSearchApi = supplierSearchApi;
    }

    List<GeocodedLocation> geocodeAndSearch(List<String> locationQueries, LocalDate checkIn, LocalDate checkOut) {
        return locationQueries.stream()
            .map(query -> {
                GeocodingService.GeoPoint point = geocodingService.geocode(query);
                boolean hasInventory = !supplierSearchApi.searchHotels(query, checkIn, checkOut).isEmpty();
                return new GeocodedLocation(query, query, point.latitude(), point.longitude(), hasInventory);
            })
            .toList();
    }
}
