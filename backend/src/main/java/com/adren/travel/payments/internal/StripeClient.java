package com.adren.travel.payments.internal;

import com.adren.travel.shared.Money;

import java.util.UUID;

/**
 * Seam over the real Stripe SDK (PRD §12.4, FIN-11) — this mock phase ships
 * only {@link StubStripeClient}; a real implementation calling Stripe's
 * PaymentIntents API is production-tier work, swapped in behind this same
 * interface without touching {@code PaymentsServiceImpl}.
 */
interface StripeClient {

    StripePaymentIntent createPaymentIntent(Money amount, UUID bookingReferenceId);
}
