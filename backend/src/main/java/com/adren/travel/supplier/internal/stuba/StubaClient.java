package com.adren.travel.supplier.internal.stuba;

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
 * STUBA integration per PRD Section 10.2.2:
 * <ul>
 *   <li>Auth: username/password-based session token (XML API), refreshed per session</li>
 *   <li>{@code OfferId} is opaque and maps to {@code supplierRateId}</li>
 *   <li>Session-expiry errors trigger automatic re-authentication and a single
 *       retry before surfacing "temporarily unavailable" (§10.2.2)</li>
 * </ul>
 * This is a stub — replace the body of {@link #search} with the real STUBA
 * XML-API call once sandbox credentials are available, and {@link
 * #acquireSessionToken()} with the real session-token request. The shape
 * (normalized return type, error contract) should not change. Mirrors
 * {@code HotelbedsClient}'s existing stub pattern.
 */
@Component
public class StubaClient {

    private final WebClient webClient;

    public StubaClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("https://api.test.stuba.com").build();
    }

    /**
     * @throws StubaSessionExpiredException if the session token has expired
     *         and re-authentication (see {@link #acquireSessionToken()})
     *         should be retried once before surfacing a user-facing failure
     *         (PRD §10.2.2).
     */
    public List<SupplierSearchResult> search(String locationCode, LocalDate checkIn, LocalDate checkOut) {
        // TODO: real call — acquire/refresh the session token via
        // acquireSessionToken(), then call STUBA's XML search API and map
        // HotelId/OfferId/RoomTypeName/MealType/SellingPrice per §10.2.2's
        // field-mapping table (note: SellingPrice may require a
        // reverse-markup calculation to derive true net rate — confirm
        // contract terms during integration).
        return List.of(new SupplierSearchResult(
            SupplierId.STUBA,
            "stub-offer-id",
            "Stub STUBA Hotel — replace with live STUBA response",
            "Standard Room",
            new Money(BigDecimal.valueOf(4800), CurrencyCode.INR),
            null // real rating requires supplier content sync (PRD §10.5) — see BOK-27
        ));
    }

    /**
     * Isolated so the real XML-API session request can replace only this
     * method without touching {@link #search}'s contract. Session tokens
     * are STUBA-specific (PRD §10.2.2) and refreshed per session.
     */
    private String acquireSessionToken() {
        // TODO: real STUBA session-token request.
        return "stub-session-token";
    }

    public static class StubaSessionExpiredException extends RuntimeException {
        public StubaSessionExpiredException(String sessionToken) {
            super("STUBA session expired, re-authentication required: " + sessionToken);
        }
    }
}
