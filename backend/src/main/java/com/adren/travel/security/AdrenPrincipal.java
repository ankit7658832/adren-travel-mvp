package com.adren.travel.security;

import java.util.UUID;

/**
 * The authenticated principal every module reads to enforce PRD Section 6's
 * role matrix and tenant isolation (RULES.md §5.1/§5.2). Carried as the
 * {@code principal} of the {@link org.springframework.security.core.Authentication}
 * set by the JWT filter chain — read it via {@link CurrentPrincipal}, never by
 * re-deriving userId/role/consultantId from raw request data.
 *
 * @param userId       the authenticated user's id
 * @param role         SUPER_ADMIN / CONSULTANT / USER
 * @param consultantId the tenant this principal belongs to; {@code null} only for SUPER_ADMIN
 */
public record AdrenPrincipal(UUID userId, Role role, UUID consultantId) {

    public AdrenPrincipal {
        if (userId == null) {
            throw new IllegalArgumentException("userId is required");
        }
        if (role == null) {
            throw new IllegalArgumentException("role is required");
        }
        if (role != Role.SUPER_ADMIN && consultantId == null) {
            throw new IllegalArgumentException("consultantId is required for role " + role);
        }
    }

    public boolean isSuperAdmin() {
        return role == Role.SUPER_ADMIN;
    }
}
