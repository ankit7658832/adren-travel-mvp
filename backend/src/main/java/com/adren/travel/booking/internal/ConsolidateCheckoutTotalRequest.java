package com.adren.travel.booking.internal;

import com.adren.travel.shared.CurrencyCode;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.Map;

record ConsolidateCheckoutTotalRequest(
    @NotNull CurrencyCode targetSellCurrency,
    Map<CurrencyCode, BigDecimal> ratesToTargetCurrency
) {
}
