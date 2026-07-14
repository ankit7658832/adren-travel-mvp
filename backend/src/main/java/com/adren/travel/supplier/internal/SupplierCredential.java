package com.adren.travel.supplier.internal;

import com.adren.travel.supplier.SupplierId;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * An Adren-owned supplier's credential (PRD §21.6/§10.2) — package-private,
 * own table. {@code secretArn} is the Secrets Manager ARN returned by
 * {@link SupplierSecretsService#storeSecret} (FND-11, RULES.md §5.3) — the
 * raw credential value itself is never persisted in Postgres, logged, or
 * returned from this module's public surface (only via
 * {@code SupplierCredentialSummary}'s masked view).
 */
@Entity
@Table(name = "supplier_credential")
class SupplierCredential {

    @Id
    @Enumerated(EnumType.STRING)
    private SupplierId supplierId;

    @Column(name = "secret_arn")
    private String secretArn;

    private UUID lastModifiedByUserId;
    private Instant lastModifiedAt;

    protected SupplierCredential() {
        // JPA
    }

    SupplierCredential(SupplierId supplierId, String secretArn, UUID lastModifiedByUserId) {
        this.supplierId = supplierId;
        this.secretArn = secretArn;
        this.lastModifiedByUserId = lastModifiedByUserId;
        this.lastModifiedAt = Instant.now();
    }

    void rotate(String secretArn, UUID lastModifiedByUserId) {
        this.secretArn = secretArn;
        this.lastModifiedByUserId = lastModifiedByUserId;
        this.lastModifiedAt = Instant.now();
    }

    SupplierId getSupplierId() {
        return supplierId;
    }

    String getSecretArn() {
        return secretArn;
    }

    UUID getLastModifiedByUserId() {
        return lastModifiedByUserId;
    }

    Instant getLastModifiedAt() {
        return lastModifiedAt;
    }
}
