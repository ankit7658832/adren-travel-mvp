package com.adren.travel.payments.event;

import com.adren.travel.shared.Money;

import java.util.UUID;

/**
 * Published when a Consultant/market's currency buffer is applied to an
 * FX-converted base rate, before markup (PRD §12.2, §12.1 Worked Example B,
 * FIN-03). Carries the base and the buffered result as two distinct
 * amounts, mirroring {@code CommissionCalculatedEvent}'s shape.
 */
public record CurrencyBufferAppliedEvent(UUID bookingId, UUID consultantId, Money fxConvertedBase, Money bufferedAmount) {
}
