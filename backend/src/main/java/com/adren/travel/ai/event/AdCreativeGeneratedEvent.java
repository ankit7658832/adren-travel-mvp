package com.adren.travel.ai.event;

import java.util.UUID;

/** Published whenever an ad-creative generation attempt completes, however it resolved (PRD §14.4, AI-12) — same shape as {@link AiSuggestionGeneratedEvent}, package-scoped instead of itinerary-scoped. */
public record AdCreativeGeneratedEvent(UUID auditLogId, UUID packageId, UUID consultantId, String disposition) {
}
