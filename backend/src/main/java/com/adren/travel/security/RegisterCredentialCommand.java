package com.adren.travel.security;

import java.util.UUID;

/**
 * Registers a real, password-checked login identity (AUTH-01) — called by
 * whichever module creates the tenant/user record this credential logs
 * into (whitelabel, for a new Consultant or ConsultantUser). {@code
 * rawPassword} is hashed inside the security module; no other module ever
 * sees or stores a password hash (RULES.md §5).
 *
 * @param principalUserId the exact id that must become {@link
 *                        AdrenPrincipal#userId()} on every future login for
 *                        this identity — the caller's own entity id (e.g.
 *                        a ConsultantUser's userId), NOT freshly minted
 *                        here, since other modules (capability grants,
 *                        audit trails) already key data off that same id
 * @param consultantId    required for CONSULTANT/USER, must be {@code null} for SUPER_ADMIN
 */
public record RegisterCredentialCommand(
    UUID principalUserId, String email, String rawPassword, Role role, UUID consultantId) {
}
