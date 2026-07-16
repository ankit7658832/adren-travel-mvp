package com.adren.travel.booking.internal;

import com.adren.travel.shared.CurrencyCode;
import com.adren.travel.supplier.SupplierId;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalTime;

record AddActivityLineItemRequest(
    @NotNull SupplierId supplierId,
    @NotNull String supplierRateId,
    @Positive int durationMinutes,
    @NotNull LocalTime timeSlot,
    @Positive int headcount,
    @NotNull BigDecimal netRate,
    @NotNull CurrencyCode netRateCurrency,
    @NotNull CurrencyCode sellCurrency,
    @NotNull BigDecimal fxRate,
    @NotNull BigDecimal bufferPercent,
    @NotNull BigDecimal commissionPercent
) {
}
