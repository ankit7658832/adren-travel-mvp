package com.adren.travel.payments;

import com.adren.travel.shared.CurrencyCode;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * An immutable, once-taken FX rate snapshot (PRD §12.2, §22.4 T7, FIN-04).
 * RULES.md §4.4 — the discipline this value object exists to enforce is in
 * never re-fetching a rate on any downstream code path: once a
 * {@code FxRateSnapshot} is captured for a booking, every later
 * calculation (currency buffer, sell rate, refund) must reuse THIS value,
 * never a freshly looked-up market rate, no matter how much time passes
 * between quotation and booking confirmation.
 */
public record FxRateSnapshot(CurrencyCode supplierCurrency, CurrencyCode sellCurrency, BigDecimal rate, Instant snapshotAt) {

    public FxRateSnapshot {
        Objects.requireNonNull(supplierCurrency, "supplierCurrency must not be null");
        Objects.requireNonNull(sellCurrency, "sellCurrency must not be null");
        Objects.requireNonNull(snapshotAt, "snapshotAt must not be null");
        if (rate == null || rate.signum() <= 0) {
            throw new IllegalArgumentException("rate must be a positive value");
        }
    }
}
