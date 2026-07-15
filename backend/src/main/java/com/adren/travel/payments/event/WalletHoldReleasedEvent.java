package com.adren.travel.payments.event;

import com.adren.travel.shared.Money;

import java.util.UUID;

/** Published when a booking's wallet hold is released back to available balance (PRD §12.3, FIN-07). */
public record WalletHoldReleasedEvent(UUID bookingId, UUID consultantId, Money amount) {
}
