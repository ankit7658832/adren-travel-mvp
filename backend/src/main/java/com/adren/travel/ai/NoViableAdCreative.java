package com.adren.travel.ai;

import java.util.UUID;

/** No candidate variant survived grounding against the Package's real name/price (PRD §14.4, AI-12). */
public record NoViableAdCreative(UUID auditLogId, String reason) implements AdCreativeGenerationResult {
}
