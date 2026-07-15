package com.adren.travel.payments;

import com.adren.travel.shared.CurrencyCode;
import com.adren.travel.shared.Money;
import com.adren.travel.shared.ProductCategory;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * Inputs to {@link PaymentsApi#calculateSellRate} (PRD §12.1 Worked
 * Examples A &amp; B, FIN-05): the supplier net rate and everything the
 * net→buffer→markup→commission pipeline needs to price one line item —
 * the FX rate to lock (§12.2/§22.4 T7), the Consultant/market currency
 * buffer (§12.2), and the commission percentage (§12.1). The Consultant's
 * markup itself is looked up from their already-configured
 * {@code MarkupRule} for {@code category} (FIN-01), not passed in here.
 */
public record CalculateSellRateCommand(
    UUID bookingId,
    UUID consultantId,
    ProductCategory category,
    Money netRate,
    CurrencyCode sellCurrency,
    BigDecimal fxRate,
    BigDecimal bufferPercent,
    BigDecimal commissionPercent
) {

    public CalculateSellRateCommand {
        Objects.requireNonNull(bookingId, "bookingId must not be null");
        Objects.requireNonNull(consultantId, "consultantId must not be null");
        Objects.requireNonNull(category, "category must not be null");
        Objects.requireNonNull(netRate, "netRate must not be null");
        Objects.requireNonNull(sellCurrency, "sellCurrency must not be null");
        if (fxRate == null || fxRate.signum() <= 0) {
            throw new IllegalArgumentException("fxRate must be a positive value");
        }
        if (bufferPercent == null || bufferPercent.signum() < 0) {
            throw new IllegalArgumentException("bufferPercent must be a non-negative value");
        }
        if (commissionPercent == null || commissionPercent.signum() < 0) {
            throw new IllegalArgumentException("commissionPercent must be a non-negative value");
        }
    }
}
