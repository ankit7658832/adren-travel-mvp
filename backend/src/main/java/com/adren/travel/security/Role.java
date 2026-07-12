package com.adren.travel.security;

/**
 * The three principal roles from PRD Section 6's Roles &amp; Permissions
 * Matrix. {@code consultant_id} semantics per role (RULES.md §5.1):
 * {@code SUPER_ADMIN}'s is always {@code null} (platform-wide, not tenant
 * scoped); {@code CONSULTANT}/{@code USER} always carry their tenant's id.
 */
public enum Role {
    SUPER_ADMIN,
    CONSULTANT,
    USER
}
