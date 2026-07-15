package com.adren.travel.payments;

import java.util.Objects;

/**
 * A (simplified, mock-phase) Stripe webhook event (PRD §12.4, FIN-11).
 * {@code type} follows Stripe's own event-type naming (e.g.
 * {@code "payment_intent.succeeded"}); unrecognized types are ignored
 * rather than rejected, since Stripe sends many event types this endpoint
 * doesn't act on.
 * <p>
 * <b>Production hardening gap (deliberately out of scope for this mock
 * phase):</b> a real integration must verify the {@code Stripe-Signature}
 * header against the raw request body before trusting {@code
 * paymentIntentId}/{@code type} — this command is constructed already
 * trusting the caller, which is only safe once that verification exists
 * in front of it.
 */
public record HandleStripeWebhookCommand(String type, String paymentIntentId) {

    public HandleStripeWebhookCommand {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(paymentIntentId, "paymentIntentId must not be null");
    }
}
