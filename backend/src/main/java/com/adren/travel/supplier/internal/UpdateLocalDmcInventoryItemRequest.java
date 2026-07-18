package com.adren.travel.supplier.internal;

import com.adren.travel.shared.CurrencyCode;
import com.adren.travel.shared.ProductCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

record UpdateLocalDmcInventoryItemRequest(
    @NotBlank String productName,
    @NotNull ProductCategory category,
    @NotNull BigDecimal netRate,
    @NotNull CurrencyCode netRateCurrency,
    @NotBlank String cancellationPolicyText,
    @NotNull LocalDate availableFrom,
    @NotNull LocalDate availableTo
) {
}
