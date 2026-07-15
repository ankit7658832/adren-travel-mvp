package com.adren.travel.payments.internal;

import com.adren.travel.payments.HandleStripeWebhookCommand;
import com.adren.travel.payments.PaymentsApi;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * PRD §12.4 — Stripe webhook receiver (FIN-11). See {@link
 * HandleStripeWebhookCommand}'s Javadoc for the {@code Stripe-Signature}
 * verification this mock phase deliberately omits in front of this
 * endpoint.
 */
@RestController
@RequestMapping("/api/v1/payments/webhooks/stripe")
class StripeWebhookController {

    private final PaymentsApi paymentsApi;

    StripeWebhookController(PaymentsApi paymentsApi) {
        this.paymentsApi = paymentsApi;
    }

    @PostMapping
    void receive(@Valid @RequestBody StripeWebhookRequest request) {
        paymentsApi.handleStripeWebhook(new HandleStripeWebhookCommand(request.type(), request.paymentIntentId()));
    }
}
