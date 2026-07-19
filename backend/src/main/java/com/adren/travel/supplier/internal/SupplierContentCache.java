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
 * A cached, scheduled-sync snapshot of one supplier's static content for one
 * property/sailing/activity (PRD §10.5) — package-private, own table.
 * Refreshed in place by {@link SupplierContentSyncService} on its
 * per-supplier cadence; never written to on the live search path (search
 * only reads from this cache to enrich results — see {@code
 * backend-best-practices} §5's caching-strategy rule: never cache live
 * pricing/availability, only static content).
 */
@Entity
@Table(name = "supplier_content_cache")
class SupplierContentCache {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "supplier_id")
    private SupplierId supplierId;

    @Column(name = "supplier_content_id")
    private String supplierContentId;

    private String name;
    private Double rating;

    @Column(name = "last_synced_at")
    private Instant lastSyncedAt;

    /** HRD-13 — the persisted, queryable staleness signal a Super Admin alert screen surfaces. */
    private boolean stale;

    protected SupplierContentCache() {
        // JPA
    }

    SupplierContentCache(SupplierId supplierId, String supplierContentId) {
        this.id = UUID.randomUUID();
        this.supplierId = supplierId;
        this.supplierContentId = supplierContentId;
    }

    void refresh(String name, Double rating) {
        this.name = name;
        this.rating = rating;
        this.lastSyncedAt = Instant.now();
    }

    void setStale(boolean stale) {
        this.stale = stale;
    }

    SupplierId getSupplierId() {
        return supplierId;
    }

    String getSupplierContentId() {
        return supplierContentId;
    }

    String getName() {
        return name;
    }

    Double getRating() {
        return rating;
    }

    Instant getLastSyncedAt() {
        return lastSyncedAt;
    }

    boolean isStale() {
        return stale;
    }
}
