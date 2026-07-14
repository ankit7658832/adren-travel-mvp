package com.adren.travel.supplier.internal.hotelbeds;

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
 * Hotelbeds integration per PRD Section 10.2.1:
 * <ul>
 *   <li>Auth: API key + SHA-256 signed request (key+secret+UTC timestamp) via {@code X-Signature} header</li>
 *   <li>{@code rateKey} is opaque and MUST be passed back unmodified at booking time</li>
 *   <li>Errors: stale rate key at booking -> force re-search, never silently re-price (PRD 10.2.1 error table)</li>
 * </ul>
 * This is a stub — replace the body of {@link #search} with the real
 * Hotelbeds Booking API call once sandbox credentials are available. The
 * shape (normalized return type, error contract) should not change.
 */
@Component
public class HotelbedsClient {

    private final WebClient webClient;

    public HotelbedsClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("https://api.test.hotelbeds.com").build();
    }

    /**
     * @throws HotelbedsRateExpiredException if a previously-returned rateKey
     *         is no longer valid at booking time (PRD 10.2.1 error table:
     *         "RATE_STALE" -> force new search, do not silently re-price).
     */
    public List<SupplierSearchResult> search(String locationCode, LocalDate checkIn, LocalDate checkOut) {
        // TODO: real call — sign request per PRD 10.2.1, map response fields
        // per the Hotelbeds field-mapping table (hotelCode, rateKey,
        // hotelName, boardName, net, cancellationPolicies[], rooms[].name).
        return List.of(new SupplierSearchResult(
            SupplierId.HOTELBEDS,
            "stub-rate-key",
            "Stub Hotel — replace with live Hotelbeds response",
            "Deluxe Room",
            new Money(BigDecimal.valueOf(5000), CurrencyCode.INR),
            null // real rating requires supplier content sync (PRD §10.5) — not wired for any supplier yet
        ));
    }

    public static class HotelbedsRateExpiredException extends RuntimeException {
        public HotelbedsRateExpiredException(String rateKey) {
            super("Hotelbeds rate key expired, re-search required: " + rateKey);
        }
    }
}
