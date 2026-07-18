package com.adren.travel.ai.internal;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * One AI suggestion attempt, permanently recorded (PRD §11.2 principle 5,
 * §24.3's 100%-logged NFR, RULES.md §6.3) — package-private, own table,
 * deliberately insert-only: no setter, no update method exists anywhere on
 * this class or {@code AiSuggestionAuditLogRepository}, and nothing in this
 * module ever calls {@code repository.save} on an entity obtained via
 * {@code findById} — every write is a fresh {@code new AiSuggestionAuditLog(...)}
 * followed by {@code saveAndFlush}. This is the durable, queryable,
 * compliance-grade record RULES.md §6.3 distinguishes from application
 * logs (which may be sampled/rotated) — AI-08's edited-final-version is a
 * SEPARATE linked {@code AiSuggestionApproval} row, not an update to this
 * one, for the same reason (the original must never be overwritten).
 * <p>
 * {@code correlationId}/{@code attemptNumber} exist for AI-13: a retried
 * Groq call (timeout/rate-limit) gets its OWN row per attempt, sharing one
 * {@code correlationId} — never one row whose {@code aiOutputJson} gets
 * overwritten attempt-to-attempt, which would silently lose the record of
 * earlier failed attempts.
 */
@Entity
@Table(name = "ai_suggestion_audit_log")
class AiSuggestionAuditLog {

    @Id
    private UUID auditLogId;

    private UUID correlationId;
    private int attemptNumber;
    private UUID consultantId;
    private UUID itineraryId;

    private String requestInputJson;
    private String sourceDataSnapshotJson;
    private String aiOutputJson;

    // AI-08: the exact, GROUNDED AiSuggestedLineItem list actually
    // returned to the caller — null unless disposition is SUGGESTED.
    // Distinct from aiOutputJson (the model's raw response) so AI-08's
    // "was it edited" comparison is apples-to-apples against what the
    // Consultant actually saw, not the model's raw text.
    private String suggestedLineItemsJson;

    @Enumerated(EnumType.STRING)
    private AiSuggestionDisposition disposition;

    private Instant createdAt;

    protected AiSuggestionAuditLog() {
        // JPA
    }

    AiSuggestionAuditLog(UUID auditLogId, UUID correlationId, int attemptNumber, UUID consultantId, UUID itineraryId,
                          String requestInputJson, String sourceDataSnapshotJson, String aiOutputJson,
                          String suggestedLineItemsJson, AiSuggestionDisposition disposition) {
        this.auditLogId = auditLogId;
        this.correlationId = correlationId;
        this.attemptNumber = attemptNumber;
        this.consultantId = consultantId;
        this.itineraryId = itineraryId;
        this.requestInputJson = requestInputJson;
        this.sourceDataSnapshotJson = sourceDataSnapshotJson;
        this.aiOutputJson = aiOutputJson;
        this.suggestedLineItemsJson = suggestedLineItemsJson;
        this.disposition = disposition;
        this.createdAt = Instant.now();
    }

    UUID getAuditLogId() {
        return auditLogId;
    }

    UUID getCorrelationId() {
        return correlationId;
    }

    int getAttemptNumber() {
        return attemptNumber;
    }

    UUID getConsultantId() {
        return consultantId;
    }

    UUID getItineraryId() {
        return itineraryId;
    }

    String getRequestInputJson() {
        return requestInputJson;
    }

    String getSourceDataSnapshotJson() {
        return sourceDataSnapshotJson;
    }

    String getAiOutputJson() {
        return aiOutputJson;
    }

    String getSuggestedLineItemsJson() {
        return suggestedLineItemsJson;
    }

    AiSuggestionDisposition getDisposition() {
        return disposition;
    }

    Instant getCreatedAt() {
        return createdAt;
    }
}
