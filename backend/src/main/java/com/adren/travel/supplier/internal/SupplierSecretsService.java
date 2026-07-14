package com.adren.travel.supplier.internal;

import com.adren.travel.supplier.SupplierId;

/**
 * FND-11 / RULES.md §5.3 — writes an Adren-owned supplier credential to
 * Secrets Manager and returns its ARN. {@link SupplierCredential} persists
 * only the returned ARN, never the raw {@code secretValue} this interface
 * receives.
 */
interface SupplierSecretsService {

    /** Creates or rotates the secret for {@code supplierId}, returning its ARN. */
    String storeSecret(SupplierId supplierId, String secretValue);
}
