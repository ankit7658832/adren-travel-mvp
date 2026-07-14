package com.adren.travel.supplier;

import com.adren.travel.shared.Money;

/**
 * Normalized line-item search result, matching the field mapping tables in
 * PRD Section 10.2 (e.g., 10.2.1 Hotelbeds' {@code rateKey} -> supplierRateId).
 * <p>
 * {@code rating} is nullable — real supplier content sync (ratings,
 * descriptions, images) is scheduled/production-tier work (PRD §10.5,
 * {@code SUP-*} stories), not yet wired for any supplier; FND-14's Default
 * Selection Algorithm treats a missing rating as the lowest tiebreak score
 * rather than failing.
 */
public record SupplierSearchResult(
    SupplierId supplierId,
    String supplierRateId,
    String propertyName,
    String roomType,
    Money netRate,
    Double rating
) {
}
