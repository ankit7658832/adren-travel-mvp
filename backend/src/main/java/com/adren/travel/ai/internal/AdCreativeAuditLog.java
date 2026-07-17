package com.adren.travel.ai.internal;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * One ad-creative generation attempt, permanently recorded (PRD §14.4,
 * §24.3's 100%-logged NFR) — package-private, own table, insert-only for
 * the same reason {@link AiSuggestionAuditLog} is (see that class's
 * Javadoc): no setter, no update method, every write is a fresh row.
 * Package-scoped ({@code packageId}, not {@code itineraryId}) rather than
 * reusing {@code ai_suggestion_audit_log} — ad-creative generation isn't
 * tied to an itinerary at all.
 */
@Entity
@Table(name = "ad_creative_audit_log")
class AdCreativeAuditLog {

    @Id
    private UUID auditLogId;

    private UUID consultantId;
    private UUID packageId;

    private String requestInputJson;
    private String sourceDataSnapshotJson;
    private String aiOutputJson;

    @Enumerated(EnumType.STRING)
    private AiSuggestionDisposition disposition;

    private Instant createdAt;

    protected AdCreativeAuditLog() {
        // JPA
    }

    AdCreativeAuditLog(UUID auditLogId, UUID consultantId, UUID packageId, String requestInputJson,
                        String sourceDataSnapshotJson, String aiOutputJson, AiSuggestionDisposition disposition) {
        this.auditLogId = auditLogId;
        this.consultantId = consultantId;
        this.packageId = packageId;
        this.requestInputJson = requestInputJson;
        this.sourceDataSnapshotJson = sourceDataSnapshotJson;
        this.aiOutputJson = aiOutputJson;
        this.disposition = disposition;
        this.createdAt = Instant.now();
    }

    UUID getAuditLogId() {
        return auditLogId;
    }

    UUID getConsultantId() {
        return consultantId;
    }

    UUID getPackageId() {
        return packageId;
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

    AiSuggestionDisposition getDisposition() {
        return disposition;
    }

    Instant getCreatedAt() {
        return createdAt;
    }
}
