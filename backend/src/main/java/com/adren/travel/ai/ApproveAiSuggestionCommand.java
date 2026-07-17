package com.adren.travel.ai;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Inputs to {@link AiApi#approveAiSuggestion} (PRD §23.3 Edge Case #8,
 * §25 T14, AI-08). {@code auditLogId} ties this approval back to the exact
 * {@code AiSuggestionAuditLog} row it's approving — never a fresh
 * client-supplied suggestion. {@code finalLineItems} is whatever the
 * Consultant is ACTUALLY approving, edited or not; comparing it against
 * the audit log's own stored {@code suggestedLineItemsJson} is how {@code
 * AiServiceImpl} determines {@code wasEdited} — the caller never declares
 * "I edited this" itself (a caller-supplied boolean would be trivially
 * wrong/spoofable; the comparison is done server-side against the
 * original).
 */
public record ApproveAiSuggestionCommand(UUID auditLogId, UUID approvedByUserId, List<AiSuggestedLineItem> finalLineItems) {

    public ApproveAiSuggestionCommand {
        Objects.requireNonNull(auditLogId, "auditLogId must not be null");
        Objects.requireNonNull(approvedByUserId, "approvedByUserId must not be null");
        Objects.requireNonNull(finalLineItems, "finalLineItems must not be null");
        if (finalLineItems.isEmpty()) {
            throw new IllegalArgumentException("finalLineItems must not be empty");
        }
    }
}
