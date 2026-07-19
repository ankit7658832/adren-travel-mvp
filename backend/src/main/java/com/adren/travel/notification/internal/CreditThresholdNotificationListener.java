package com.adren.travel.notification.internal;

import com.adren.travel.payments.event.CreditThresholdBreachedEvent;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * PRD §15, HRD-02 — "credit threshold" trigger, published by the {@code
 * payments} module whenever a wallet hold attempt is rejected for
 * exceeding {@code availableBalance + creditLimit} (FIN-08).
 */
@Component
class CreditThresholdNotificationListener {

    private final NotificationDispatcher dispatcher;

    CreditThresholdNotificationListener(NotificationDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @ApplicationModuleListener
    void on(CreditThresholdBreachedEvent event) {
        String subject = "Wallet credit limit reached";
        String body = "A booking attempt of " + event.attemptedAmount() + " could not be held — you have reached "
            + "your wallet balance plus credit limit. Please top up to continue.";
        String message = "Credit limit reached — top up your wallet to continue booking.";
        dispatcher.dispatch(event.consultantId(), subject, body, message);
    }
}
