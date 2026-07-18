package com.adren.travel.booking.internal;

import com.adren.travel.shared.CurrencyCode;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

/** {@code budgetAmount}/{@code budgetCurrency} are both null together (no budget constraint) or both present. */
record GenerateAiSuggestionRequest(
    @NotNull String locationCode,
    @NotNull LocalDate checkIn,
    @NotNull LocalDate checkOut,
    @NotNull String naturalLanguageRequest,
    BigDecimal budgetAmount,
    CurrencyCode budgetCurrency
) {
}
