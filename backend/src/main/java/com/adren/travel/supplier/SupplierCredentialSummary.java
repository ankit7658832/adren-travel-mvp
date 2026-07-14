package com.adren.travel.supplier;

import java.time.Instant;
import java.util.UUID;

/**
 * Masked view of a supplier credential (PRD §21.6) — never the secret
 * value itself, only whether one is configured and who last changed it.
 */
public record SupplierCredentialSummary(
    SupplierId supplierId, boolean configured, UUID lastModifiedByUserId, Instant lastModifiedAt) {
}
