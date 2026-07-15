package com.adren.travel.payments;

import com.adren.travel.shared.Money;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * Inputs to {@link PaymentsApi#applyCurrencyBuffer} (PRD §12.2, §12.1
 * Worked Example B, FIN-03): the FX-converted base rate for one booking and
 * the Consultant/market-configured buffer percentage (2-5%) to protect
 * against FX movement between quotation and payment — applied BEFORE the
 * Consultant's markup (FIN-01), per Worked Example B's INR 9,600 → 9,888
 * step.
 */
public record ApplyCurrencyBufferCommand(UUID bookingId, UUID consultantId, Money fxConvertedBase, BigDecimal bufferPercent) {

    public ApplyCurrencyBufferCommand {
        Objects.requireNonNull(bookingId, "bookingId must not be null");
        Objects.requireNonNull(consultantId, "consultantId must not be null");
        Objects.requireNonNull(fxConvertedBase, "fxConvertedBase must not be null");
        if (bufferPercent == null || bufferPercent.signum() < 0) {
            throw new IllegalArgumentException("bufferPercent must be a non-negative value");
        }
    }
}
