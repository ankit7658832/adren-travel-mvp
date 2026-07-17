package com.adren.travel.payments.internal;

/**
 * What a {@link PaymentIntentRecord} settles (FIN-15) — the Stripe webhook
 * handler branches on this: {@code BOOKING} confirms a booking (FIN-11's
 * original path); {@code WALLET_TOP_UP} reconciles a wallet top-up instead
 * (PRD §23.4 Edge Case #10), crediting {@code availableBalance} only once
 * the webhook actually arrives — never at PaymentIntent creation time,
 * which is what keeps an unconfirmed top-up from being bookable against.
 */
enum PaymentIntentPurpose {
    BOOKING,
    WALLET_TOP_UP
}
