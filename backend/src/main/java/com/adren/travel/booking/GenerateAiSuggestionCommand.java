package com.adren.travel.booking;

import com.adren.travel.shared.Money;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Inputs to {@link BookingApi#generateAiItinerarySuggestion} (PRD §11.1,
 * AI-02) — thin booking-facing wrapper around {@code
 * com.adren.travel.ai.GenerateItineraryCommand}'s fields; {@code
 * consultantId}/{@code itineraryId} are resolved from the owned DRAFT
 * itinerary itself (same "caller supplies structured input, resolves
 * ownership server-side" shape every other booking command uses), not
 * caller-supplied here.
 */
public record GenerateAiSuggestionCommand(
    String locationCode,
    LocalDate checkIn,
    LocalDate checkOut,
    String naturalLanguageRequest,
    Money budgetLimit
) {

    public GenerateAiSuggestionCommand {
        Objects.requireNonNull(locationCode, "locationCode must not be null");
        Objects.requireNonNull(checkIn, "checkIn must not be null");
        Objects.requireNonNull(checkOut, "checkOut must not be null");
        Objects.requireNonNull(naturalLanguageRequest, "naturalLanguageRequest must not be null");
    }
}
