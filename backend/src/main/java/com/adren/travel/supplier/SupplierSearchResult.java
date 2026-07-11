package com.adren.travel.supplier;

import com.adren.travel.shared.Money;

/**
 * Normalized line-item search result, matching the field mapping tables in
 * PRD Section 10.2 (e.g., 10.2.1 Hotelbeds' {@code rateKey} -> supplierRateId).
 */
public record SupplierSearchResult(
    SupplierId supplierId,
    String supplierRateId,
    String propertyName,
    String roomType,
    Money netRate
) {
    public enum SupplierId { HOTELBEDS, STUBA, TBO, LOCAL_DMC, BYOS }
}
