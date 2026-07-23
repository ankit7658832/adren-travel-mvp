package com.adren.travel.security.internal;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * SCR-00b (doc/ADREN_UIUX_SPEC.md §5.2) — a one-time, expiring password
 * reset token. {@code tokenId} doubles as the token value itself embedded
 * in the reset link (a random UUID is unguessable enough for this mock
 * phase — same reasoning every other id in this codebase already relies
 * on, RULES.md §5).
 */
@Entity
@Table(name = "password_reset_token")
class PasswordResetToken {

    @Id
    private UUID tokenId;

    private UUID credentialId;
    private Instant expiresAt;
    private Instant usedAt;
    private Instant createdAt;

    protected PasswordResetToken() {
        // JPA
    }

    PasswordResetToken(UUID tokenId, UUID credentialId, Instant expiresAt) {
        this.tokenId = tokenId;
        this.credentialId = credentialId;
        this.expiresAt = expiresAt;
        this.createdAt = Instant.now();
    }

    UUID getTokenId() {
        return tokenId;
    }

    UUID getCredentialId() {
        return credentialId;
    }

    boolean isUsable() {
        return usedAt == null && Instant.now().isBefore(expiresAt);
    }

    void markUsed() {
        this.usedAt = Instant.now();
    }
}
