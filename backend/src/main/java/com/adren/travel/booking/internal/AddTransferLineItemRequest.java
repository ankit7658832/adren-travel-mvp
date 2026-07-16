package com.adren.travel.booking.internal;

import com.adren.travel.shared.CurrencyCode;
import com.adren.travel.supplier.SupplierId;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

record AddTransferLineItemRequest(
    @NotNull SupplierId supplierId,
    @NotNull String supplierRateId,
    @NotNull String vehicleType,
    @NotNull String pickupPoint,
    @NotNull String dropoffPoint,
    @NotNull BigDecimal netRate,
    @NotNull CurrencyCode netRateCurrency,
    @NotNull CurrencyCode sellCurrency,
    @NotNull BigDecimal fxRate,
    @NotNull BigDecimal bufferPercent,
    @NotNull BigDecimal commissionPercent
) {
}
