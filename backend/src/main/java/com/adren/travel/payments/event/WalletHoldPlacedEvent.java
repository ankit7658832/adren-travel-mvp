package com.adren.travel.payments.event;

import com.adren.travel.shared.Money;

import java.util.UUID;

/** Published when a booking places a hold on a Consultant's wallet (PRD §12.3, FIN-07). */
public record WalletHoldPlacedEvent(UUID bookingId, UUID consultantId, Money amount) {
}
