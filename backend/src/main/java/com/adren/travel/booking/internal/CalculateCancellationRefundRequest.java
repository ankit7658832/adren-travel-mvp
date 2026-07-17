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
    @NotNull BigDecimal postDeadlinePenaltyPercent,
    // FIN-14 — the booking's ORIGINAL FX rate snapshot (FIN-04), so the
    // refund's supplier-currency conversion reuses it rather than a
    // freshly looked-up rate (PRD §23.4 Edge Case #9/T15).
    @NotNull CurrencyCode originalSupplierCurrency,
    @NotNull BigDecimal originalFxRate,
    @NotNull Instant originalFxSnapshotAt
) {
}
