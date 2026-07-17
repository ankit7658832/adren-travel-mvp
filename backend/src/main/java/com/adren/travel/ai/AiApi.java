package com.adren.travel.ai;

import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Public API of the AI Itinerary &amp; Governance module (PRD §11). Other
 * modules must depend on this interface, never on classes under
 * {@code com.adren.travel.ai.internal}.
 */
public interface AiApi {

    /**
     * Generates a grounded itinerary suggestion (PRD §11.1/§11.2) —
     * invoked from {@code BookingApi.generateAiItinerarySuggestion} (the
     * "Complete with AI" entry point's backend, BOK's {@code Itinerary}
     * being the aggregate this suggestion is FOR). Same tenant-scoping
     * shape as every other tenant-scoped write — {@code consultantId} is
     * checked against the caller's own tenant via {@code
     * CurrentPrincipal.resolveTenantScope}. Always returns (never throws
     * for "no viable option" — see {@link AiItineraryGenerationResult}'s
     * Javadoc); only throws for genuine infrastructure failure (Groq
     * unreachable/misconfigured — PRD §11.2 principle 4's "explicit
     * failure state" is about business outcomes, not about masking a real
     * system fault as if it were a normal empty result).
     */
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','CONSULTANT','USER')")
    AiItineraryGenerationResult generateItinerary(GenerateItineraryCommand command);
}
