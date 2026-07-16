package com.adren.travel.supplier.internal.widgety;

import com.adren.travel.shared.CurrencyCode;
import com.adren.travel.shared.Money;
import com.adren.travel.supplier.SupplierId;
import com.adren.travel.supplier.SupplierSearchResult;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Widgety (cruise) integration per PRD Section 10.2.6:
 * <ul>
 *   <li>Auth: API key-based, partner-tier access levels</li>
 *   <li>{@code SailingId} maps to {@code supplierRateId}; Widgety's own
 *       multi-port {@code Itinerary} structure is flattened into a single
 *       line item with port-by-port detail carried as metadata (see {@link
 *       WidgetySearchResult#ports()}), not separate line items</li>
 *   <li>"Cabin category sold out" is a distinct, common failure mode (unlike
 *       hotels' "rate expired") — modeled as {@link WidgetyCabinSoldOutException}</li>
 * </ul>
 * This is a stub — replace the body of {@link #search} with the real Widgety
 * REST call once sandbox credentials are available. Mirrors {@code
 * HotelbedsClient}'s existing stub pattern.
 */
@Component
public class WidgetyClient {

    private final WebClient webClient;

    public WidgetyClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("https://api.test.widgety.com").build();
    }

    /**
     * @throws WidgetyCabinSoldOutException if the requested cabin category is
     *         sold out for this sailing (PRD §10.2.6) — distinct from a plain
     *         empty result.
     */
    public List<WidgetySearchResult> search(String embarkationPort, LocalDate departureDate) {
        // TODO: real call — map SailingId/CabinCategory/CruiseLine per
        // §10.2.6's field-mapping table, flattening Widgety's multi-port
        // Itinerary structure into this stub's ports list.
        SupplierSearchResult normalized = new SupplierSearchResult(
            SupplierId.WIDGETY,
            "stub-sailing-id",
            "Stub Cruise Line — replace with live Widgety response",
            "Balcony Cabin",
            new Money(BigDecimal.valueOf(35000), CurrencyCode.INR),
            null // real rating requires supplier content sync (PRD §10.5) — see BOK-27
        );
        return List.of(new WidgetySearchResult(normalized, List.of("Stub Port A", "Stub Port B")));
    }

    /**
     * @param normalized the common cross-supplier result shape.
     * @param ports port-by-port detail from Widgety's own multi-port
     *              itinerary structure, flattened into this single line item
     *              as metadata rather than separate line items (PRD §10.2.6).
     */
    public record WidgetySearchResult(SupplierSearchResult normalized, List<String> ports) {
    }

    public static class WidgetyCabinSoldOutException extends RuntimeException {
        public WidgetyCabinSoldOutException(String sailingId, String cabinCategory) {
            super("Widgety cabin category sold out for sailing " + sailingId + ": " + cabinCategory);
        }
    }
}
