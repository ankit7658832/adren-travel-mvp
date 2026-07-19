package com.adren.travel.notification.internal;

import com.adren.travel.booking.event.DisputeTicketCreatedEvent;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * PRD §12.5, FIN-16 — "a dispute create a tracked ticket, not just an
 * email handoff": the ticket ({@code DisputeTicket}, booking module) IS
 * the tracked record; this listener is the notification fan-out on top of
 * it, alerting the Consultant a ticket now exists — via {@link NotificationDispatcher},
 * the same region-routed email + WhatsApp/SMS shape every trigger-event
 * listener uses. HRD-03: {@code disputeTicketId} is the dedup key.
 */
@Component
class DisputeTicketNotificationListener {

    private static final String LISTENER_NAME = "DisputeTicketNotificationListener";

    private final NotificationDispatcher dispatcher;
    private final ProcessedEventDeduplicationService deduplicationService;

    DisputeTicketNotificationListener(NotificationDispatcher dispatcher, ProcessedEventDeduplicationService deduplicationService) {
        this.dispatcher = dispatcher;
        this.deduplicationService = deduplicationService;
    }

    @ApplicationModuleListener
    void on(DisputeTicketCreatedEvent event) {
        if (!deduplicationService.tryClaim(event.disputeTicketId().toString(), LISTENER_NAME)) {
            return;
        }

        String subject = "A dispute has been flagged on your booking";
        String body = "Dispute ticket " + event.disputeTicketId() + " was opened for booking "
            + event.bookingId() + ": " + event.reason();
        String message = "Dispute opened for booking " + event.bookingId() + ".";
        dispatcher.dispatch(event.consultantId(), subject, body, message);
    }
}
