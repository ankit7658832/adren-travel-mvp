package com.adren.travel.supplier.internal;

import com.adren.travel.supplier.SupplierSearchApi;
import com.adren.travel.supplier.SupplierSearchResult;
import com.adren.travel.supplier.internal.hotelbeds.HotelbedsClient;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Aggregates all connected suppliers behind {@link SupplierSearchApi}.
 * Per PRD Section 24.2 (NFR): each supplier call is isolated so one
 * supplier's downtime doesn't degrade the others — in the full
 * implementation, wrap each call below in its own circuit breaker
 * (e.g., Resilience4j) rather than calling them inline as shown in this
 * scaffold.
 */
@Service
class SupplierAggregationService implements SupplierSearchApi {

    private final HotelbedsClient hotelbedsClient;
    // TODO: inject StubaClient, TboClient, LocalDmcRepository, ByosClient
    // as each is built out, following the HotelbedsClient pattern
    // (PRD Section 10.2.2 - 10.2.9).

    SupplierAggregationService(HotelbedsClient hotelbedsClient) {
        this.hotelbedsClient = hotelbedsClient;
    }

    @Override
    public List<SupplierSearchResult> searchHotels(String locationCode, LocalDate checkIn, LocalDate checkOut) {
        List<SupplierSearchResult> results = new ArrayList<>();
        try {
            results.addAll(hotelbedsClient.search(locationCode, checkIn, checkOut));
        } catch (Exception e) {
            // PRD 10.2.1: on timeout, exclude Hotelbeds from this cycle rather
            // than failing the whole search.
        }
        // TODO: merge STUBA/TBO/Local DMC/BYOS results here, then apply the
        // deduplication + Default Selection Algorithm (PRD Section 9.2, 9.4).
        return results;
    }
}
