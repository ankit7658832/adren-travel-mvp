package com.adren.travel.payments;

import com.adren.travel.shared.Money;

import java.util.Objects;
import java.util.UUID;

/**
 * Inputs to {@link PaymentsApi#calculateIndiaGstTcs} (PRD §12.1 Worked
 * Example C, §17.2, FIN-17) — an outbound package sold to an India-based
 * Consultant's traveler. {@code marginAmount} is the Consultant's
 * margin/service component (what GST applies to); {@code packageValue} is
 * the full outbound package value (what TCS applies to, above the
 * notified threshold) — two different bases, per §12.1's own distinction.
 */
public record CalculateIndiaGstTcsCommand(UUID bookingId, UUID consultantId, Money marginAmount, Money packageValue) {

    public CalculateIndiaGstTcsCommand {
        Objects.requireNonNull(bookingId, "bookingId must not be null");
        Objects.requireNonNull(consultantId, "consultantId must not be null");
        Objects.requireNonNull(marginAmount, "marginAmount must not be null");
        Objects.requireNonNull(packageValue, "packageValue must not be null");
    }
}
