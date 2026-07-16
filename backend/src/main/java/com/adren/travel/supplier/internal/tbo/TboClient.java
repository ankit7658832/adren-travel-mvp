package com.adren.travel.supplier.internal.tbo;

import com.adren.travel.shared.CurrencyCode;
import com.adren.travel.shared.Money;
import com.adren.travel.supplier.SupplierId;
import com.adren.travel.supplier.SupplierSearchResult;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * TBO integration per PRD Section 10.2.3:
 * <ul>
 *   <li>Auth: API key, IP whitelisting, separate test/UAT vs. production credentials</li>
 *   <li>{@code ResultIndex} maps to {@code supplierRateId}; {@code TraceId} must be
 *       reused across every call within one search session — this is threaded
 *       explicitly through {@link #search}'s signature/return value rather than
 *       held as hidden client state, so the caller (the in-progress itinerary
 *       draft) can persist and reuse it across the search-to-booking window</li>
 *   <li>An expired {@code TraceId} requires a full re-search, never a partial retry</li>
 * </ul>
 * This is a stub — replace the body of {@link #search} with the real TBO API
 * call once sandbox credentials are available. The shape (normalized return
 * type, error contract) should not change. Mirrors {@code HotelbedsClient}'s
 * existing stub pattern.
 */
@Component
public class TboClient {

    private final WebClient webClient;

    public TboClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("https://api.test.tbo.com").build();
    }

    /**
     * @param traceId the session's existing TraceId, or {@code null} to start
     *                a new search session (a fresh TraceId is generated).
     * @throws TboTraceIdExpiredException if the given {@code traceId} is no
     *         longer valid — callers must force a full re-search (PRD §10.2.3),
     *         never a partial retry with the same TraceId.
     */
    public TboSearchResponse search(String locationCode, LocalDate checkIn, LocalDate checkOut, String traceId) {
        // TODO: real call — pass locationCode/checkIn/checkOut plus traceId
        // (or request a new one if null) to TBO's search API, mapping
        // HotelCode/ResultIndex/DayRates per §10.2.3's field-mapping table.
        String effectiveTraceId = traceId != null ? traceId : UUID.randomUUID().toString();
        List<SupplierSearchResult> results = List.of(new SupplierSearchResult(
            SupplierId.TBO,
            "stub-result-index",
            "Stub TBO Hotel — replace with live TBO response",
            "Deluxe Room",
            new Money(BigDecimal.valueOf(4500), CurrencyCode.INR),
            null // real rating requires supplier content sync (PRD §10.5) — see BOK-27
        ));
        return new TboSearchResponse(results, effectiveTraceId);
    }

    /**
     * @param results the normalized search results for this call.
     * @param traceId the TraceId to reuse for every subsequent call within
     *                the same search session (PRD §10.2.3) — the caller must
     *                persist this against the in-progress itinerary draft.
     */
    public record TboSearchResponse(List<SupplierSearchResult> results, String traceId) {
    }

    public static class TboTraceIdExpiredException extends RuntimeException {
        public TboTraceIdExpiredException(String traceId) {
            super("TBO TraceId expired, full re-search required: " + traceId);
        }
    }
}
