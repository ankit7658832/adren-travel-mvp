package com.adren.travel.payments;

/** Lifecycle of a Stripe PaymentIntent this module tracks (PRD §12.4, §24.4, FIN-11). */
public enum PaymentIntentStatus {
    REQUIRES_PAYMENT_METHOD, SUCCEEDED, FAILED
}
