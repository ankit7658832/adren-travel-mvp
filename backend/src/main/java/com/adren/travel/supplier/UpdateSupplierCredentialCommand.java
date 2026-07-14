package com.adren.travel.supplier;

/** Cross-module-safe input — never persists the raw value in a log (RULES.md §5.3/§6.2). */
public record UpdateSupplierCredentialCommand(SupplierId supplierId, String secretValue) {
}
