package com.adren.travel.payments;

import com.adren.travel.shared.Money;

/**
 * A created PaymentIntent (PRD §12.4, FIN-11) — cross-module-safe, never
 * the JPA entity itself. {@code clientSecret} is what the frontend passes
 * to Stripe's hosted payment element; it is never logged (RULES.md §6.2 —
 * treat it like any other bearer credential).
 */
public record PaymentIntentView(String paymentIntentId, String clientSecret, Money amount, PaymentIntentStatus status) {
}
