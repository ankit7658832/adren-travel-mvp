package com.adren.travel.payments.event;

import com.adren.travel.shared.Money;

import java.util.UUID;

/**
 * Published when Adren's commission on a booking's supplier net rate is
 * calculated (PRD §12.1 Worked Example A, FIN-02). Carries {@code netRate}
 * and {@code commissionAmount} as two distinct amounts — never netted
 * against the Consultant's markup (FIN-01) — so the ledger can attribute
 * each separately, per this story's acceptance criterion.
 */
public record CommissionCalculatedEvent(UUID bookingId, UUID consultantId, Money netRate, Money commissionAmount) {
}
