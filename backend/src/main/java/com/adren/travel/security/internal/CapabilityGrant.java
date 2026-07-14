package com.adren.travel.security.internal;

import com.adren.travel.security.CapabilityGrantService.Capability;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * A single per-User capability grant — the data-driven backing for PRD
 * Section 6's "No (unless granted)" matrix cells (RULES.md §5.1). One row
 * per (userId, capability); presence of the row with {@code granted=true} is
 * the grant.
 */
@Entity
@Table(name = "capability_grant")
class CapabilityGrant {

    @Id
    private UUID id;

    private UUID userId;

    @Enumerated(EnumType.STRING)
    private Capability capability;

    private boolean granted;

    protected CapabilityGrant() {
        // JPA
    }

    CapabilityGrant(UUID id, UUID userId, Capability capability, boolean granted) {
        this.id = id;
        this.userId = userId;
        this.capability = capability;
        this.granted = granted;
    }

    boolean isGranted() {
        return granted;
    }

    void setGranted(boolean granted) {
        this.granted = granted;
    }
}
