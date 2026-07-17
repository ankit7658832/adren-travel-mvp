package com.adren.travel.booking.internal;

/**
 * State machine for {@link CancellationRequest} (PRD §12.5, FIN-16).
 * {@code PENDING_APPROVAL} is only reached when {@code
 * RefundCalculation.requiresConsultantApproval()} is true (a penalty
 * applies); a penalty-free cancellation skips straight to {@code
 * APPROVED} — there is no Consultant decision to make when nothing is
 * being withheld.
 */
enum CancellationStatus {
    PENDING_APPROVAL,
    APPROVED,
    REFUNDED
}
