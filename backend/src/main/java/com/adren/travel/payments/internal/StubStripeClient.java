package com.adren.travel.payments.internal;

import com.adren.travel.payments.PaymentIntentStatus;
import com.adren.travel.shared.Money;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Mock-phase-only {@link StripeClient} — generates a fake PaymentIntent id
 * and client secret rather than calling Stripe's real API, matching the
 * user's confirmed FIN-11 approach (mock/stub the Stripe client behind an
 * interface). Always starts a new PaymentIntent at
 * {@code REQUIRES_PAYMENT_METHOD}; the webhook handler is what later
 * transitions it, exactly as the real Stripe integration would.
 */
@Component
class StubStripeClient implements StripeClient {

    @Override
    public StripePaymentIntent createPaymentIntent(Money amount, UUID bookingReferenceId) {
        String paymentIntentId = "pi_stub_" + UUID.randomUUID();
        String clientSecret = paymentIntentId + "_secret_" + UUID.randomUUID();
        return new StripePaymentIntent(paymentIntentId, clientSecret, amount, PaymentIntentStatus.REQUIRES_PAYMENT_METHOD);
    }
}
