package com.adren.travel.booking;

import com.adren.travel.shared.CurrencyCode;

import java.math.BigDecimal;

/**
 * One alternate product a Consultant can swap a location's line item to
 * (PRD §21.2, FND-16) — a booking-owned façade over
 * {@code supplier.SupplierSearchResult}, never that type itself, so this
 * module's public surface doesn't change shape if the supplier module's
 * internal result type does (mirrors how {@code GeocodedLocation} already
 * only exposes a plain {@code autoSelectedSupplierRateId} string rather
 * than the full supplier result).
 */
public record AlternateOption(
    String supplierId,
    String supplierRateId,
    String propertyName,
    String roomType,
    BigDecimal netRateAmount,
    CurrencyCode netRateCurrency,
    Double rating
) {
}
