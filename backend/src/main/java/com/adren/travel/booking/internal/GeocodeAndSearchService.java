package com.adren.travel.booking.internal;

import com.adren.travel.supplier.SupplierId;
import com.adren.travel.supplier.SupplierSearchApi;
import com.adren.travel.supplier.SupplierSearchResult;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * Backs PRD §9.1 Flow A steps 2-4: geocode every location the Consultant
 * typed, check whether any supplier has inventory there — one location
 * with no inventory must still render its own pin (T1), so this never
 * throws or drops a location, it marks it {@code hasInventory: false} —
 * and runs FND-14's Default Selection Algorithm over whatever inventory
 * exists to pre-select a per-location default.
 */
@Service
class GeocodeAndSearchService {

    private final GeocodingService geocodingService;
    private final SupplierSearchApi supplierSearchApi;
    private final DefaultSelectionService defaultSelectionService;

    GeocodeAndSearchService(GeocodingService geocodingService, SupplierSearchApi supplierSearchApi,
                             DefaultSelectionService defaultSelectionService) {
        this.geocodingService = geocodingService;
        this.supplierSearchApi = supplierSearchApi;
        this.defaultSelectionService = defaultSelectionService;
    }

    List<GeocodedLocation> geocodeAndSearch(List<String> locationQueries, LocalDate checkIn, LocalDate checkOut) {
        return locationQueries.stream()
            .map(query -> {
                GeocodingService.GeoPoint point = geocodingService.geocode(query);
                List<SupplierSearchResult> options = supplierSearchApi.searchHotels(query, checkIn, checkOut);
                // No per-Consultant preferred-supplier config exists yet
                // (FIN-01, Financial Layer) — the algorithm already honors
                // one once that lands, nothing here needs to change.
                SupplierId noPreferenceConfiguredYet = null;
                String autoSelectedSupplierRateId = defaultSelectionService.selectDefault(options, noPreferenceConfiguredYet)
                    .map(SupplierSearchResult::supplierRateId)
                    .orElse(null);
                return new GeocodedLocation(query, query, point.latitude(), point.longitude(),
                    !options.isEmpty(), autoSelectedSupplierRateId);
            })
            .toList();
    }
}
