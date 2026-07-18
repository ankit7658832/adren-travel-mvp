package com.adren.travel.ai;

import java.util.List;
import java.util.UUID;

/** At least one variant survived grounding (PRD §14.4, AI-12) — {@code auditLogId} links back to the permanent audit trail. */
public record AdCreativeSuggestion(UUID auditLogId, List<AdCreativeVariant> variants) implements AdCreativeGenerationResult {
}
