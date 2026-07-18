package com.adren.travel.supplier;

import java.time.Instant;

/**
 * Masked view of the calling Consultant's own BYOS credential (PRD §10.4,
 * DMC-06) — never the secret value itself, mirroring
 * {@link SupplierCredentialSummary}'s shape for Adren-owned credentials.
 */
public record ByosCredentialSummary(SupplierId supplierId, boolean configured, Instant lastModifiedAt) {
}
