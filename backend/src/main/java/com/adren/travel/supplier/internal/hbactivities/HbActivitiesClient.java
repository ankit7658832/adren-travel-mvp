package com.adren.travel.supplier.internal.hbactivities;

import com.adren.travel.shared.CurrencyCode;
import com.adren.travel.shared.Money;
import com.adren.travel.supplier.SupplierId;
import com.adren.travel.supplier.SupplierSearchResult;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * HBActivities integration per PRD Section 10.2.7:
 * <ul>
 *   <li>Auth: API key-based, REST</li>
 *   <li>{@code ActivityId} maps to {@code supplierRateId}; availability is
 *       time-slot based (distinct from hotels' date-range availability) —
 *       represented explicitly via {@link ActivityTimeSlot} rather than a
 *       single per-day flag, so a specific sold-out slot can be distinguished
 *       from a fully-unavailable day (PRD §10.2.7)</li>
 * </ul>
 * This is a stub — replace the body of {@link #search} with the real
 * HBActivities REST call once sandbox credentials are available. Mirrors
 * {@code HotelbedsClient}'s existing stub pattern.
 */
@Component
public class HbActivitiesClient {

    private final WebClient webClient;

    public HbActivitiesClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("https://api.test.hbactivities.com").build();
    }

    public List<HbActivitiesSearchResult> search(String locationCode, LocalDate date) {
        // TODO: real call — map ActivityId/ActivityName/DurationMinutes/
        // Price/AvailableSlots[] per §10.2.7's field-mapping table.
        SupplierSearchResult normalized = new SupplierSearchResult(
            SupplierId.HBACTIVITIES,
            "stub-activity-id",
            "Stub Activity — replace with live HBActivities response",
            null,
            new Money(BigDecimal.valueOf(2000), CurrencyCode.INR),
            null // real rating requires supplier content sync (PRD §10.5) — see BOK-27
        );
        List<ActivityTimeSlot> slots = List.of(
            new ActivityTimeSlot(LocalTime.of(9, 0), 8),
            new ActivityTimeSlot(LocalTime.of(14, 0), 0) // sold out — distinct from a fully-unavailable day
        );
        return List.of(new HbActivitiesSearchResult(normalized, slots));
    }

    /**
     * @param normalized the common cross-supplier result shape.
     * @param availableSlots time-slot-based availability (PRD §10.2.7) —
     *                       {@code availableCount == 0} means that specific
     *                       slot is sold out, not that the whole day is.
     */
    public record HbActivitiesSearchResult(SupplierSearchResult normalized, List<ActivityTimeSlot> availableSlots) {
    }

    public record ActivityTimeSlot(LocalTime time, int availableCount) {
    }
}
