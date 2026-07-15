package com.adren.travel.supplier.internal;

import com.adren.travel.supplier.SupplierId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

/**
 * A Consultant's own BYOS supplier credential (PRD §10.4, FND-12) —
 * package-private, own table. Row-level, per-Consultant encryption (never
 * the shared Secrets-Manager-by-ARN pattern FND-11 uses for Adren's own
 * credentials): {@code ciphertext}/{@code iv}/{@code wrappedDataKey} are
 * {@link KmsEnvelopeEncryptionService}'s output — the plaintext credential
 * is never persisted, logged, or returned from this module's public
 * surface.
 */
@Entity
@Table(name = "byos_credential", uniqueConstraints = @UniqueConstraint(columnNames = {"consultant_id", "supplier_id"}))
class ByosCredential {

    @Id
    private UUID id;

    private UUID consultantId;

    @Enumerated(EnumType.STRING)
    private SupplierId supplierId;

    @Column(name = "ciphertext")
    private byte[] ciphertext;

    @Column(name = "iv")
    private byte[] iv;

    @Column(name = "wrapped_data_key")
    private byte[] wrappedDataKey;

    private UUID lastModifiedByUserId;
    private Instant lastModifiedAt;

    protected ByosCredential() {
        // JPA
    }

    ByosCredential(UUID id, UUID consultantId, SupplierId supplierId, byte[] ciphertext, byte[] iv,
                   byte[] wrappedDataKey, UUID lastModifiedByUserId) {
        this.id = id;
        this.consultantId = consultantId;
        this.supplierId = supplierId;
        this.ciphertext = ciphertext;
        this.iv = iv;
        this.wrappedDataKey = wrappedDataKey;
        this.lastModifiedByUserId = lastModifiedByUserId;
        this.lastModifiedAt = Instant.now();
    }

    void rotate(byte[] ciphertext, byte[] iv, byte[] wrappedDataKey, UUID lastModifiedByUserId) {
        this.ciphertext = ciphertext;
        this.iv = iv;
        this.wrappedDataKey = wrappedDataKey;
        this.lastModifiedByUserId = lastModifiedByUserId;
        this.lastModifiedAt = Instant.now();
    }

    UUID getId() {
        return id;
    }

    UUID getConsultantId() {
        return consultantId;
    }

    SupplierId getSupplierId() {
        return supplierId;
    }

    byte[] getCiphertext() {
        return ciphertext;
    }

    byte[] getIv() {
        return iv;
    }

    byte[] getWrappedDataKey() {
        return wrappedDataKey;
    }

    UUID getLastModifiedByUserId() {
        return lastModifiedByUserId;
    }

    Instant getLastModifiedAt() {
        return lastModifiedAt;
    }
}
