package com.adren.travel.payments.event;

import com.adren.travel.shared.Money;

import java.util.UUID;

/** Published whenever a cancellation's refund/penalty split is calculated (PRD §12.4/§12.5, FIN-13) — a calculation, not a money movement. */
public record RefundCalculatedEvent(
    UUID bookingId, UUID consultantId, Money refundAmount, Money penaltyAmount, boolean requiresConsultantApproval) {
}
