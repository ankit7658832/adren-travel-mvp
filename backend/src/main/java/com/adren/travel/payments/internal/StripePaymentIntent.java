package com.adren.travel.payments.internal;

import com.adren.travel.payments.PaymentIntentStatus;
import com.adren.travel.shared.Money;

/** What {@link StripeClient#createPaymentIntent} returns — mirrors Stripe's own PaymentIntent shape, trimmed to what this module needs. */
record StripePaymentIntent(String paymentIntentId, String clientSecret, Money amount, PaymentIntentStatus status) {
}
