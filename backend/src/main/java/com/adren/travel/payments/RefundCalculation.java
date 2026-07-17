package com.adren.travel.payments;

import com.adren.travel.shared.Money;

/**
 * The result of {@link PaymentsApi#calculateRefund} (PRD §12.4/§12.5,
 * FIN-13). {@code requiresConsultantApproval} is true whenever a penalty
 * applies (cancellation after the supplier's deadline) — per the PRD AC,
 * a penalized refund must not move money until a Consultant has reviewed
 * this calculation; this record's job is the calculation, not enforcing
 * the approval gate itself (a full approval-then-execute workflow is
 * HRD-05's "full cancellation workflow" scope, not this story's).
 * {@code refundAmountInSupplierCurrency} is {@code refundAmount} converted
 * back into the original supplier currency using the booking's original
 * {@code FxRateSnapshot} (FIN-14, PRD §23.4 Edge Case #9/T15) — never a
 * freshly looked-up rate, even if the market rate moved between booking
 * and cancellation.
 */
public record RefundCalculation(Money refundAmount, Money penaltyAmount, boolean requiresConsultantApproval,
    Money refundAmountInSupplierCurrency) {
}
