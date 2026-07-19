package com.adren.travel.notification.internal;

import com.adren.travel.payments.event.StripePaymentSucceededEvent;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * PRD §15, HRD-02 — "payment received" trigger. Fires on {@link StripePaymentSucceededEvent}
 * (the webhook-confirmed money-received moment that GATES {@code confirmBooking}),
 * not {@code BookingPaidOnAccountEvent} — On-Account billing defers payment
 * rather than receiving it now, so it is not this trigger (PRD §21.4/§20.8,
 * FIN-12's own framing). HRD-03: {@code bookingReferenceId} is the dedup
 * key — {@code handleStripeWebhook}'s own {@code alreadyReconciled} guard
 * (FIN-15) already prevents this event from being published twice for a
 * genuine webhook retry, but this listener still claims it independently
 * rather than relying on that upstream guarantee.
 */
@Component
class PaymentReceivedNotificationListener {

    private static final String LISTENER_NAME = "PaymentReceivedNotificationListener";

    private final NotificationDispatcher dispatcher;
    private final ProcessedEventDeduplicationService deduplicationService;

    PaymentReceivedNotificationListener(NotificationDispatcher dispatcher, ProcessedEventDeduplicationService deduplicationService) {
        this.dispatcher = dispatcher;
        this.deduplicationService = deduplicationService;
    }

    @ApplicationModuleListener
    void on(StripePaymentSucceededEvent event) {
        if (!deduplicationService.tryClaim(event.bookingReferenceId().toString(), LISTENER_NAME)) {
            return;
        }

        String subject = "Payment received";
        String body = "We received your payment of " + event.amount() + " for booking reference "
            + event.bookingReferenceId() + ".";
        String message = "Payment of " + event.amount() + " received.";
        dispatcher.dispatch(event.consultantId(), subject, body, message);
    }
}
