package com.adren.travel.ai;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * The result of {@link AiApi#generateItinerary} (PRD §11.2 principle 4,
 * AI-05) — a sealed, exhaustively-matched result type rather than an
 * exception for the "no viable suggestion" case. "No inventory/no option
 * fits the budget" is a legitimate, expected outcome of grounded
 * generation, not a system failure — modeling it as a typed value (not a
 * thrown exception a generic error handler somewhere up the call stack
 * could catch-and-paper-over into a misleading message) is what makes "AI
 * states inability rather than substituting" enforceable by the compiler,
 * not just documentation. {@code @JsonTypeInfo}/{@code @JsonSubTypes} give
 * the REST response an explicit {@code "type"} discriminator field
 * ({@code "SUGGESTION"}/{@code "NO_VIABLE_SUGGESTION"}) so the frontend
 * never has to infer which outcome it got from field presence alone.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = AiItinerarySuggestion.class, name = "SUGGESTION"),
    @JsonSubTypes.Type(value = NoViableSuggestion.class, name = "NO_VIABLE_SUGGESTION")
})
public sealed interface AiItineraryGenerationResult permits AiItinerarySuggestion, NoViableSuggestion {
}
