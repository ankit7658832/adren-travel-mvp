package com.adren.travel.notification.internal;

import com.adren.travel.payments.event.StripePaymentSucceededEvent;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * PRD §15, HRD-02 — "payment received" trigger. Fires on {@link StripePaymentSucceededEvent}
 * (the webhook-confirmed money-received moment that GATES {@code confirmBooking}),
 * not {@code BookingPaidOnAccountEvent} — On-Account billing defers payment
 * rather than receiving it now, so it is not this trigger (PRD §21.4/§20.8,
 * FIN-12's own framing).
 */
@Component
class PaymentReceivedNotificationListener {

    private final NotificationDispatcher dispatcher;

    PaymentReceivedNotificationListener(NotificationDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @ApplicationModuleListener
    void on(StripePaymentSucceededEvent event) {
        String subject = "Payment received";
        String body = "We received your payment of " + event.amount() + " for booking reference "
            + event.bookingReferenceId() + ".";
        String message = "Payment of " + event.amount() + " received.";
        dispatcher.dispatch(event.consultantId(), subject, body, message);
    }
}
