package com.adren.travel.payments;

import com.adren.travel.shared.Money;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * Inputs to {@link PaymentsApi#calculateCommission} (PRD §12.1 Worked
 * Example A, FIN-02): the supplier net rate for one booking and the
 * commission percentage Adren takes on it, kept separate from the
 * Consultant's own markup calculation (FIN-01).
 */
public record CalculateCommissionCommand(UUID bookingId, UUID consultantId, Money netRate, BigDecimal commissionPercent) {

    public CalculateCommissionCommand {
        Objects.requireNonNull(bookingId, "bookingId must not be null");
        Objects.requireNonNull(consultantId, "consultantId must not be null");
        Objects.requireNonNull(netRate, "netRate must not be null");
        if (commissionPercent == null || commissionPercent.signum() < 0) {
            throw new IllegalArgumentException("commissionPercent must be a non-negative value");
        }
    }
}
