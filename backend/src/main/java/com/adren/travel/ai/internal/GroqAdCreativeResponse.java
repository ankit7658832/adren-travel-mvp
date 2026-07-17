package com.adren.travel.ai.internal;

import java.util.List;

/**
 * The structured JSON contract the ad-creative system prompt requires
 * Groq to respond with — a plain list of {@code {headline, bodyText}}
 * pairs. Unlike {@link GroqSuggestionResponse}'s deliberately-minimal
 * id-only contract, the model here DOES author descriptive text (that's
 * the point — ad copy), so grounding is enforced differently: {@code
 * AiServiceImpl#groundAdCreativeVariants} checks every returned {@code
 * bodyText} literally contains the real package name and exact current
 * sell price, dropping any variant that doesn't, rather than trusting the
 * model not to drift from the real values.
 */
record GroqAdCreativeResponse(List<GroqAdCreativeVariantResponse> variants) {

    record GroqAdCreativeVariantResponse(String headline, String bodyText) {
    }
}
