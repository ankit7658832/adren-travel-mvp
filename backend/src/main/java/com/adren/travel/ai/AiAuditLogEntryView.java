package com.adren.travel.ai;

import java.time.Instant;
import java.util.UUID;

/**
 * One row of the AI suggestion audit trail (PRD §6, §21.6, AI-11) — never
 * the JPA entity itself. {@code disposition} is one of {@code
 * AiSuggestionDisposition}'s names as a plain {@code String} rather than
 * an exposed enum type, since that enum is {@code internal} (RULES.md
 * §4.1) — the same shape {@code AiSuggestionGeneratedEvent.disposition}
 * already uses.
 */
public record AiAuditLogEntryView(
    UUID auditLogId,
    UUID correlationId,
    int attemptNumber,
    UUID consultantId,
    UUID itineraryId,
    String requestInputJson,
    String sourceDataSnapshotJson,
    String aiOutputJson,
    String disposition,
    Instant createdAt
) {
}
