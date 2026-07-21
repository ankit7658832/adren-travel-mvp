package com.adren.travel.security.internal;

import com.adren.travel.security.Role;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * One row per loginable identity across all three roles (AUTH-01) —
 * package-private, own table (RULES.md §4.2). {@code credentialId} doubles
 * as the {@link com.adren.travel.security.AdrenPrincipal#userId()} minted
 * into that identity's JWT on every successful login.
 */
@Entity
@Table(name = "principal_credential")
class PrincipalCredential {

    @Id
    private UUID credentialId;

    private String email;
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    private Role role;

    private UUID consultantId;
    private Instant createdAt;

    protected PrincipalCredential() {
        // JPA
    }

    PrincipalCredential(UUID credentialId, String email, String passwordHash, Role role, UUID consultantId) {
        this.credentialId = credentialId;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.consultantId = consultantId;
        this.createdAt = Instant.now();
    }

    UUID getCredentialId() {
        return credentialId;
    }

    String getEmail() {
        return email;
    }

    String getPasswordHash() {
        return passwordHash;
    }

    Role getRole() {
        return role;
    }

    UUID getConsultantId() {
        return consultantId;
    }
}
