package com.adren.travel.payments.internal;

import com.adren.travel.shared.CurrencyCode;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

record CreatePaymentIntentRequest(
    @NotNull UUID bookingReferenceId,
    @NotNull UUID consultantId,
    @NotNull BigDecimal amount,
    @NotNull CurrencyCode currency
) {
}
