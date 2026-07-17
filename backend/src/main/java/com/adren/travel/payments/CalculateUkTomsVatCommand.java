package com.adren.travel.payments;

import com.adren.travel.shared.Money;

import java.util.Objects;
import java.util.UUID;

/**
 * Inputs to {@link PaymentsApi#calculateUkTomsVat} (PRD §12.1 Worked
 * Example D, §17.2, FIN-18). {@code marginAmount} is the Consultant's
 * package cost/margin under the TOMS mechanism — TOMS VAT applies to this
 * only, never the full package sale price (the exact distinction Example D
 * exists to make: "must not be approximated as a flat percentage of total
 * sale price").
 */
public record CalculateUkTomsVatCommand(UUID bookingId, UUID consultantId, Money marginAmount) {

    public CalculateUkTomsVatCommand {
        Objects.requireNonNull(bookingId, "bookingId must not be null");
        Objects.requireNonNull(consultantId, "consultantId must not be null");
        Objects.requireNonNull(marginAmount, "marginAmount must not be null");
    }
}
