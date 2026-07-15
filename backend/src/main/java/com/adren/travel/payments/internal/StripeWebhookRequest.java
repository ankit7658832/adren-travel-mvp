package com.adren.travel.payments.internal;

import jakarta.validation.constraints.NotBlank;

record StripeWebhookRequest(@NotBlank String type, @NotBlank String paymentIntentId) {
}
