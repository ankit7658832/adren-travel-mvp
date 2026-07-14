package com.adren.travel.whitelabel.internal;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * A staff/sub-agent User under a Consultant's own account (PRD §3.3) —
 * package-private, own table (RULES.md §4.2). Capability grants (e.g.
 * "create package") live in the {@code security} module's own
 * {@code capability_grant} table, keyed by this entity's {@code userId}.
 */
@Entity
@Table(name = "consultant_user")
class ConsultantUser {

    @Id
    private UUID userId;

    private UUID consultantId;
    private String email;
    private String displayName;
    private Instant createdAt;

    protected ConsultantUser() {
        // JPA
    }

    ConsultantUser(UUID userId, UUID consultantId, String email, String displayName) {
        this.userId = userId;
        this.consultantId = consultantId;
        this.email = email;
        this.displayName = displayName;
        this.createdAt = Instant.now();
    }

    UUID getUserId() {
        return userId;
    }

    UUID getConsultantId() {
        return consultantId;
    }

    String getEmail() {
        return email;
    }

    String getDisplayName() {
        return displayName;
    }
}
