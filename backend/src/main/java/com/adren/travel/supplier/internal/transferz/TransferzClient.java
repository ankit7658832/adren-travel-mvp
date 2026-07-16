package com.adren.travel.supplier.internal.transferz;

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
 * Transferz integration per PRD Section 10.2.5:
 * <ul>
 *   <li>Auth: API key, REST-based</li>
 *   <li>{@code TransferOptionId} maps to {@code supplierRateId}</li>
 *   <li>"No coverage at this route" is a distinct case from "no availability"
 *       (§10.2.5) and must produce a different user-facing message — modeled
 *       here as {@link TransferzNoCoverageException} rather than collapsed
 *       into an empty result list</li>
 * </ul>
 * This is a stub — replace the body of {@link #search} with the real
 * Transferz REST call once sandbox credentials are available. The shape
 * (normalized return type, error contract) should not change. Mirrors
 * {@code HotelbedsClient}'s existing stub pattern.
 */
@Component
public class TransferzClient {

    private final WebClient webClient;

    public TransferzClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("https://api.test.transferz.com").build();
    }

    /**
     * @throws TransferzNoCoverageException if Transferz does not service the
     *         given pickup/dropoff pair at all (distinct from a plain empty
     *         result, which means "no availability right now" — PRD §10.2.5).
     */
    public List<SupplierSearchResult> search(String pickupPoint, String dropoffPoint, LocalDate date) {
        // TODO: real call — map TransferOptionId/VehicleType/PickupPoint/
        // DropoffPoint/Price per §10.2.5's field-mapping table; throw
        // TransferzNoCoverageException when Transferz reports no coverage
        // for this route, rather than returning an empty result set.
        return List.of(new SupplierSearchResult(
            SupplierId.TRANSFERZ,
            "stub-transfer-option-id",
            "Stub Transfer — replace with live Transferz response",
            "Sedan",
            new Money(BigDecimal.valueOf(1200), CurrencyCode.INR),
            null
        ));
    }

    public static class TransferzNoCoverageException extends RuntimeException {
        public TransferzNoCoverageException(String pickupPoint, String dropoffPoint) {
            super("Transferz does not service this route: " + pickupPoint + " -> " + dropoffPoint);
        }
    }
}
