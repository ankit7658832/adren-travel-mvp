package com.adren.travel.payments.internal;

import com.adren.travel.payments.MarkupType;
import com.adren.travel.shared.CurrencyCode;
import com.adren.travel.shared.ProductCategory;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

record ConfigureMarkupRequest(
    @NotNull ProductCategory category,
    @NotNull MarkupType markupType,
    BigDecimal percentageValue,
    BigDecimal flatFeeAmount,
    CurrencyCode flatFeeCurrency
) {
}
