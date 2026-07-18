package com.adren.travel.ai.internal;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * A Consultant's approval of one AI suggestion (AI-08, PRD §23.3 Edge Case
 * #8/§25 T14) — package-private, own table, insert-only for the same
 * reason {@link AiSuggestionAuditLog} is: {@code editedFinalVersionJson}
 * is a SEPARATE linked row referencing {@link AiSuggestionAuditLog} by
 * {@code auditLogId} (a plain UUID, never an object-graph reference —
 * RULES.md §4.1), never an update to the original audit row, so both the
 * original AI output and whatever the Consultant actually approved remain
 * independently queryable forever.
 */
@Entity
@Table(name = "ai_suggestion_approval")
class AiSuggestionApproval {

    @Id
    private UUID approvalId;

    private UUID auditLogId;
    private UUID approvedByUserId;
    private String editedFinalVersionJson;
    private boolean wasEdited;
    private Instant approvedAt;

    protected AiSuggestionApproval() {
        // JPA
    }

    AiSuggestionApproval(UUID approvalId, UUID auditLogId, UUID approvedByUserId, String editedFinalVersionJson,
                          boolean wasEdited) {
        this.approvalId = approvalId;
        this.auditLogId = auditLogId;
        this.approvedByUserId = approvedByUserId;
        this.editedFinalVersionJson = editedFinalVersionJson;
        this.wasEdited = wasEdited;
        this.approvedAt = Instant.now();
    }

    UUID getApprovalId() {
        return approvalId;
    }

    UUID getAuditLogId() {
        return auditLogId;
    }

    UUID getApprovedByUserId() {
        return approvedByUserId;
    }

    String getEditedFinalVersionJson() {
        return editedFinalVersionJson;
    }

    boolean isWasEdited() {
        return wasEdited;
    }

    Instant getApprovedAt() {
        return approvedAt;
    }
}
