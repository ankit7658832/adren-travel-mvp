package com.adren.travel.ai.event;

import java.util.UUID;

/** Published whenever an AI generation attempt completes, however it resolved (PRD §11.2 principle 5, AI-02/AI-07) — {@code disposition} is one of {@code AiSuggestionDisposition}'s names, kept as a plain String since that enum is internal (RULES.md §4.1). */
public record AiSuggestionGeneratedEvent(UUID auditLogId, UUID itineraryId, UUID consultantId, String disposition) {
}
