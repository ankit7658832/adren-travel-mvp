package com.adren.travel.ai;

import com.adren.travel.shared.Money;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Inputs to {@link AiApi#generateItinerary} (PRD §11.1, AI-02).
 * {@code locationCode} is caller-supplied structured input (the same
 * location string {@code SupplierSearchApi.searchHotels} and {@code
 * findAlternates} already take) rather than parsed out of {@code
 * naturalLanguageRequest} — this module has no NL-to-location extraction
 * capability, and grounding the search on an unreliable extraction would
 * undermine the "grounded generation only" principle before the LLM call
 * even happens. {@code naturalLanguageRequest} still drives tone/
 * preference framing in the prompt ("family-friendly", "budget-conscious")
 * — it is never the source of which candidates are eligible, only how the
 * model is asked to rank/describe them. {@code budgetLimit} is nullable —
 * no budget constraint if absent.
 */
public record GenerateItineraryCommand(
    UUID consultantId,
    UUID itineraryId,
    String locationCode,
    LocalDate checkIn,
    LocalDate checkOut,
    String naturalLanguageRequest,
    Money budgetLimit
) {

    public GenerateItineraryCommand {
        Objects.requireNonNull(consultantId, "consultantId must not be null");
        Objects.requireNonNull(itineraryId, "itineraryId must not be null");
        Objects.requireNonNull(locationCode, "locationCode must not be null");
        Objects.requireNonNull(checkIn, "checkIn must not be null");
        Objects.requireNonNull(checkOut, "checkOut must not be null");
        Objects.requireNonNull(naturalLanguageRequest, "naturalLanguageRequest must not be null");
        if (naturalLanguageRequest.isBlank()) {
            throw new IllegalArgumentException("naturalLanguageRequest must not be blank");
        }
    }
}
