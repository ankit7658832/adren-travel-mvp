package com.adren.travel.booking;

import com.adren.travel.ai.AiSuggestedLineItem;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Inputs to {@link BookingApi#approveAiSuggestion} (PRD §11.2 principle 3,
 * §23.3 Edge Case #8, AI-06/AI-08). {@code auditLogId} identifies which of
 * the itinerary's AI generation attempts is being approved (the itinerary
 * only ever tracks its LATEST one, {@code Itinerary.aiAuditLogId}, but
 * this is caller-supplied rather than re-derived so the caller's own
 * "Complete with AI" screen state — whichever suggestion it's showing —
 * is always what gets approved, not whatever the itinerary happens to
 * point at by the time the approval request lands). {@code finalLineItems}
 * is what the Consultant is actually approving, edited from the original
 * suggestion or not.
 */
public record ApproveAiSuggestionCommand(UUID auditLogId, List<AiSuggestedLineItem> finalLineItems) {

    public ApproveAiSuggestionCommand {
        Objects.requireNonNull(auditLogId, "auditLogId must not be null");
        Objects.requireNonNull(finalLineItems, "finalLineItems must not be null");
        if (finalLineItems.isEmpty()) {
            throw new IllegalArgumentException("finalLineItems must not be empty");
        }
    }
}
