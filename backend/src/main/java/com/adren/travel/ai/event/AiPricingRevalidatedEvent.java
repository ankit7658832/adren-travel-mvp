package com.adren.travel.ai.event;

import java.util.UUID;

/** Published whenever {@code AiApi.revalidateAiPricingAtBooking} completes (PRD §11.3, AI-09) — {@code reason} is null when {@code stale} is false. */
public record AiPricingRevalidatedEvent(UUID auditLogId, UUID itineraryId, UUID consultantId, boolean stale, String reason) {
}
