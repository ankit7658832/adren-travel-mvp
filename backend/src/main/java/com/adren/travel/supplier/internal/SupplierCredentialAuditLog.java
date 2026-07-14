package com.adren.travel.supplier.internal;

import com.adren.travel.supplier.SupplierId;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Insert-only audit trail of who changed which supplier credential and
 * when (PRD §21.6) — never the secret value itself (RULES.md §6.2).
 */
@Entity
@Table(name = "supplier_credential_audit_log")
class SupplierCredentialAuditLog {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    private SupplierId supplierId;

    private UUID changedByUserId;
    private Instant changedAt;

    protected SupplierCredentialAuditLog() {
        // JPA
    }

    SupplierCredentialAuditLog(UUID id, SupplierId supplierId, UUID changedByUserId) {
        this.id = id;
        this.supplierId = supplierId;
        this.changedByUserId = changedByUserId;
        this.changedAt = Instant.now();
    }
}
