package com.adren.travel.security;

import java.util.UUID;

/**
 * Backs the "No (unless granted)" cells of PRD Section 6's role matrix (e.g.
 * a USER creating a Package) with data, not a hardcoded role {@code switch}
 * (RULES.md §5.1) — a Consultant grants a specific capability to a specific
 * User, and {@code @PreAuthorize} expressions call {@link #isGranted} by bean
 * name (e.g. {@code @capabilityGrantService.isGranted(...)}).
 */
public interface CapabilityGrantService {

    /**
     * @param userId     the USER (never a CONSULTANT/SUPER_ADMIN — grants are
     *                   only meaningful for the "unless granted" role)
     * @param capability a stable capability key, e.g. {@link Capability#CREATE_PACKAGE}
     */
    boolean isGranted(UUID userId, Capability capability);

    /**
     * Sets (or revokes) a capability grant for a User — called by whichever
     * module owns "who can grant what" (whitelabel, for a Consultant
     * granting their own User a capability per FND-09).
     */
    void setGranted(UUID userId, Capability capability, boolean granted);

    enum Capability {
        CREATE_PACKAGE
    }
}
