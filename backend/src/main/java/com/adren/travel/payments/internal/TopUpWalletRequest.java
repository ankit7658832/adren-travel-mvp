package com.adren.travel.payments.internal;

import com.adren.travel.shared.CurrencyCode;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

record TopUpWalletRequest(
    UUID consultantId,
    @Positive BigDecimal amount,
    @NotNull CurrencyCode currency
) {
}
