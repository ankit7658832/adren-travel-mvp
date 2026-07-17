package com.adren.travel.ai;

import java.util.List;
import java.util.UUID;

/**
 * A grounded AI suggestion (PRD §11.1/§11.2). {@code auditLogId} is the
 * {@code AiSuggestionAuditLog} row this suggestion is provably tied to
 * (AI-07) — {@link AiApi#approveAiSuggestion} references it, never a fresh
 * client-supplied suggestion payload, so approval can only ever apply to
 * something that was actually logged.
 */
public record AiItinerarySuggestion(UUID auditLogId, List<AiSuggestedLineItem> lineItems)
    implements AiItineraryGenerationResult {
}
