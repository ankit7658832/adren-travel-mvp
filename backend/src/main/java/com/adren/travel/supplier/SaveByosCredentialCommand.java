package com.adren.travel.supplier;

/**
 * PRD §10.4, DMC-06 — a Consultant's own supplier credential. The
 * consultantId is deliberately absent here: it is always resolved from the
 * calling principal, never accepted from the caller (RULES.md §5.2),
 * mirroring {@link UpdateSupplierCredentialCommand}'s "never log the raw
 * value" discipline.
 */
public record SaveByosCredentialCommand(SupplierId supplierId, String secretValue) {
}
