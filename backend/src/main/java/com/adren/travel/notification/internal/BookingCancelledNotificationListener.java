package com.adren.travel.notification.internal;

import com.adren.travel.booking.event.BookingCancelledEvent;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * HRD-05 — notification dispatch on refund completion (PRD §12.5/§22.7).
 * {@link BookingCancelledEvent}'s own Javadoc flagged this as HRD-05's
 * scope, not HRD-02's — the cancellation workflow itself (policy check →
 * refund/penalty calculation → approval-if-penalized → refund) was
 * already fully orchestrated by {@code submitCancellation}/{@code
 * approveCancellation} (FIN-16); this listener is the one piece that was
 * still missing. HRD-03: {@code bookingId} is the dedup key — a booking
 * only ever gets cancelled once (the entity throws on a repeat
 * transition), so a redelivery of this event carries the same bookingId.
 */
@Component
class BookingCancelledNotificationListener {

    private static final String LISTENER_NAME = "BookingCancelledNotificationListener";

    private final NotificationDispatcher dispatcher;
    private final ProcessedEventDeduplicationService deduplicationService;

    BookingCancelledNotificationListener(NotificationDispatcher dispatcher,
                                         ProcessedEventDeduplicationService deduplicationService) {
        this.dispatcher = dispatcher;
        this.deduplicationService = deduplicationService;
    }

    @ApplicationModuleListener
    void on(BookingCancelledEvent event) {
        if (!deduplicationService.tryClaim(event.bookingId().toString(), LISTENER_NAME)) {
            return;
        }

        String subject = "Your booking has been cancelled";
        String body = "Booking " + event.bookingId() + " has been cancelled. Refund: " + event.refundAmount()
            + (event.penaltyAmount().amount().signum() > 0 ? " (penalty applied: " + event.penaltyAmount() + ")." : ".");
        String message = "Booking " + event.bookingId() + " cancelled — refund " + event.refundAmount() + ".";
        dispatcher.dispatch(event.consultantId(), subject, body, message);
    }
}
