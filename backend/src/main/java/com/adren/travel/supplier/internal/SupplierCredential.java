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
 * An Adren-owned supplier's credential (PRD §21.6/§10.2) — package-private,
 * own table. {@code secretValue} is a placeholder for FND-11's Secrets
 * Manager wiring (which replaces this column with an ARN reference) — it
 * is never returned or logged raw from this module's public surface today
 * either, only via {@code SupplierCredentialSummary}'s masked view.
 */
@Entity
@Table(name = "supplier_credential")
class SupplierCredential {

    @Id
    @Enumerated(EnumType.STRING)
    private SupplierId supplierId;

    private String secretValue;
    private UUID lastModifiedByUserId;
    private Instant lastModifiedAt;

    protected SupplierCredential() {
        // JPA
    }

    SupplierCredential(SupplierId supplierId, String secretValue, UUID lastModifiedByUserId) {
        this.supplierId = supplierId;
        this.secretValue = secretValue;
        this.lastModifiedByUserId = lastModifiedByUserId;
        this.lastModifiedAt = Instant.now();
    }

    void rotate(String secretValue, UUID lastModifiedByUserId) {
        this.secretValue = secretValue;
        this.lastModifiedByUserId = lastModifiedByUserId;
        this.lastModifiedAt = Instant.now();
    }

    SupplierId getSupplierId() {
        return supplierId;
    }

    UUID getLastModifiedByUserId() {
        return lastModifiedByUserId;
    }

    Instant getLastModifiedAt() {
        return lastModifiedAt;
    }
}
