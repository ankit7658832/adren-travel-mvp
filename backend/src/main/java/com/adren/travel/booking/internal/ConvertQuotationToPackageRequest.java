package com.adren.travel.booking.internal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

record ConvertQuotationToPackageRequest(
    @NotBlank String name,
    String description,
    @NotNull LocalDate validityStart,
    @NotNull LocalDate validityEnd,
    @NotNull BigDecimal markupPrice,
    @NotNull Integer maxPax
) {
}
