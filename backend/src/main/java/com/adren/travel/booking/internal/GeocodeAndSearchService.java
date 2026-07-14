package com.adren.travel.booking.internal;

import com.adren.travel.security.CurrentPrincipal;
import com.adren.travel.supplier.SupplierId;
import com.adren.travel.supplier.SupplierSearchApi;
import com.adren.travel.supplier.SupplierSearchResult;
import com.adren.travel.whitelabel.WhitelabelApi;
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
    private final WhitelabelApi whitelabelApi;

    GeocodeAndSearchService(GeocodingService geocodingService, SupplierSearchApi supplierSearchApi,
                             DefaultSelectionService defaultSelectionService, WhitelabelApi whitelabelApi) {
        this.geocodingService = geocodingService;
        this.supplierSearchApi = supplierSearchApi;
        this.defaultSelectionService = defaultSelectionService;
        this.whitelabelApi = whitelabelApi;
    }

    List<GeocodedLocation> geocodeAndSearch(List<String> locationQueries, LocalDate checkIn, LocalDate checkOut) {
        // FND-05 — a SUSPENDED Consultant's Users can no longer search;
        // SUPER_ADMIN has no consultantId and is exempt from this gate.
        var principal = CurrentPrincipal.get();
        if (!principal.isSuperAdmin()) {
            whitelabelApi.requireConsultantActive(principal.consultantId());
        }
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
