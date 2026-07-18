package com.adren.travel.ai;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * The result of {@link AiApi#generateAdCreative} (PRD §14.4, AI-12) — same
 * explicit-typed-result shape as {@link AiItineraryGenerationResult}
 * (backend-best-practices §7): "no variant survived grounding" is a
 * legitimate outcome (every candidate the model produced either omitted
 * the real package name or referenced a price other than the current sell
 * price), not a system failure, so it's a typed value rather than a thrown
 * exception.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = AdCreativeSuggestion.class, name = "AD_CREATIVE_SUGGESTION"),
    @JsonSubTypes.Type(value = NoViableAdCreative.class, name = "NO_VIABLE_AD_CREATIVE")
})
public sealed interface AdCreativeGenerationResult permits AdCreativeSuggestion, NoViableAdCreative {
}
