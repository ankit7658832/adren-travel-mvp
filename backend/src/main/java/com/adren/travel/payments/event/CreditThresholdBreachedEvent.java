package com.adren.travel.payments.event;

import com.adren.travel.shared.Money;

import java.util.UUID;

/**
 * Published whenever a wallet hold attempt is rejected because it would
 * exceed {@code availableBalance + creditLimit} (PRD §15, HRD-02) — the
 * notification-trigger counterpart to {@link com.adren.travel.payments.CreditLimitExceededException},
 * which blocks the booking itself (FIN-08). {@code attemptedAmount} is the
 * hold that was rejected, not a wallet balance snapshot — {@code Wallet}
 * stays package-private. {@code bookingId} is also HRD-03's dedup key for
 * the notification listener: a redelivered breach for the same rejected
 * hold attempt must not double-notify.
 */
public record CreditThresholdBreachedEvent(UUID bookingId, UUID consultantId, Money attemptedAmount) {
}
