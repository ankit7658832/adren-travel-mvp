package com.adren.travel.payments.event;

import com.adren.travel.shared.Money;

import java.util.UUID;

/** Published when a booking's wallet hold resolves into an actual debit on confirmation (PRD §12.3, FIN-07). */
public record WalletHoldDebitedEvent(UUID bookingId, UUID consultantId, Money amount) {
}
