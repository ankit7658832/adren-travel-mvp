package com.adren.travel.booking.internal;

import com.adren.travel.shared.CurrencyCode;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

record ConfirmBookingRequest(
    @NotNull UUID quotationOrPackageId,
    @NotNull BigDecimal totalSellPrice,
    @NotNull CurrencyCode currency
) {
}
