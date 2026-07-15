package com.adren.travel.payments;

import com.adren.travel.shared.CurrencyCode;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * Inputs to {@link PaymentsApi#snapshotFxRate} (PRD §12.2, §22.4 T7,
 * FIN-04): the currency pair and the rate observed at the moment a quote
 * is generated. {@code rate} is supplied by the caller (an FX rate
 * source/provider is outside this story's scope) — this command's job is
 * only to capture that value into an immutable, never-re-fetched
 * {@link FxRateSnapshot}.
 */
public record SnapshotFxRateCommand(UUID bookingId, UUID consultantId, CurrencyCode supplierCurrency,
                                     CurrencyCode sellCurrency, BigDecimal rate) {

    public SnapshotFxRateCommand {
        Objects.requireNonNull(bookingId, "bookingId must not be null");
        Objects.requireNonNull(consultantId, "consultantId must not be null");
        Objects.requireNonNull(supplierCurrency, "supplierCurrency must not be null");
        Objects.requireNonNull(sellCurrency, "sellCurrency must not be null");
        if (rate == null || rate.signum() <= 0) {
            throw new IllegalArgumentException("rate must be a positive value");
        }
    }
}
