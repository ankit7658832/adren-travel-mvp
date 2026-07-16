package com.adren.travel.payments;

import java.util.UUID;

/**
 * Thrown when a booking's total would push a Consultant's wallet past
 * {@code availableBalance + creditLimit} (PRD §22.4 T8, §12.3, FIN-08) — a
 * normal, anticipated business-rule block (mapped to 409 with an
 * actionable "top up required" message), not an unexpected failure.
 */
public class CreditLimitExceededException extends RuntimeException {

    public CreditLimitExceededException(UUID consultantId) {
        super("Booking blocked: this would exceed your wallet balance plus available credit. "
            + "Please top up your wallet to continue. (consultant " + consultantId + ")");
    }
}
