package com.adren.travel.payments.event;

import com.adren.travel.shared.Money;

import java.util.UUID;

/** Published once a wallet top-up's Stripe webhook confirms it (PRD §23.4 Edge Case #10, FIN-15) — availableBalance is only credited here, never earlier. */
public record WalletTopUpReconciledEvent(UUID consultantId, Money amount) {
}
