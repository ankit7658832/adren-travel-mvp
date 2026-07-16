package com.adren.travel.booking.internal;

import com.adren.travel.shared.CurrencyCode;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;

record CalculateCancellationRefundRequest(
    @NotNull BigDecimal sellPrice,
    @NotNull CurrencyCode currency,
    @NotNull Instant cancellationDeadline,
    @NotNull Instant cancelledAt,
    @NotNull BigDecimal postDeadlinePenaltyPercent
) {
}
