package com.adren.travel.supplier.event;

import com.adren.travel.supplier.SupplierId;

import java.util.UUID;

/**
 * Published whenever a Consultant saves/rotates their own BYOS supplier
 * credential (PRD §10.4, DMC-06) — never carries the secret value itself,
 * only the fact that a rotation happened, for the same reason
 * {@code SupplierCredentialAuditLog} never persists the raw Adren-owned
 * secret (RULES.md §5.3/§6.2).
 */
public record ByosCredentialSavedEvent(UUID consultantId, SupplierId supplierId) {
}
