package com.adren.travel.booking.internal;

import com.adren.travel.booking.MealPlan;
import com.adren.travel.shared.CurrencyCode;
import com.adren.travel.supplier.SupplierId;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;

record AddHotelLineItemRequest(
    @NotNull SupplierId supplierId,
    @NotNull String supplierRateId,
    @NotNull String propertyName,
    @NotNull String roomType,
    @NotNull MealPlan mealPlan,
    @NotNull Instant cancellationDeadline,
    @NotNull BigDecimal netRate,
    @NotNull CurrencyCode netRateCurrency,
    @NotNull CurrencyCode sellCurrency,
    @NotNull BigDecimal fxRate,
    @NotNull BigDecimal bufferPercent,
    @NotNull BigDecimal commissionPercent
) {
}
