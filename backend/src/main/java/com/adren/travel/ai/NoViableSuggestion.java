package com.adren.travel.ai;

import java.util.UUID;

/**
 * "The AI states inability rather than substituting" (PRD §11.2 principle
 * 4, §23.3 Edge Case #7, §25 T13) as a legitimate typed result, not an
 * exception — see {@link AiItineraryGenerationResult}'s Javadoc for why.
 * {@code auditLogId} is still populated: a "no viable suggestion" outcome
 * is logged with exactly the same 100%-logging discipline as a successful
 * one (AI-07) — the audit trail records every AI interaction, not only the
 * ones that produced a usable result.
 */
public record NoViableSuggestion(UUID auditLogId, String reason) implements AiItineraryGenerationResult {
}
