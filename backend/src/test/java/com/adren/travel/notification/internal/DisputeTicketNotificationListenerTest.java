package com.adren.travel.notification.internal;

import com.adren.travel.booking.event.DisputeTicketCreatedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * FIN-16's dispute-ticket AC: the listener dispatches with the right
 * consultant/message; region-routing is {@link NotificationDispatcher}'s
 * own concern, tested there (same shape as {@link BookingNotificationListenerTest}).
 * HRD-03 — a redelivery of the same event (same disputeTicketId) is a no-op.
 */
@ExtendWith(MockitoExtension.class)
class DisputeTicketNotificationListenerTest {

    @Mock
    NotificationDispatcher dispatcher;

    @Mock
    ProcessedEventDeduplicationService deduplicationService;

    DisputeTicketNotificationListener listener;

    @Test
    void dispatchesOnDisputeTicketCreatedWithTheReasonInTheEmailBody() {
        listener = new DisputeTicketNotificationListener(dispatcher, deduplicationService);
        UUID consultantId = UUID.randomUUID();
        UUID disputeTicketId = UUID.randomUUID();
        when(deduplicationService.tryClaim(disputeTicketId.toString(), "DisputeTicketNotificationListener")).thenReturn(true);
        DisputeTicketCreatedEvent event = new DisputeTicketCreatedEvent(
            disputeTicketId, UUID.randomUUID(), consultantId, "Wrong room type delivered");

        listener.on(event);

        verify(dispatcher).dispatch(eq(consultantId), any(), contains("Wrong room type delivered"), any());
    }

    @Test
    void aRedeliveredEventIsANoOpHRD03() {
        listener = new DisputeTicketNotificationListener(dispatcher, deduplicationService);
        UUID disputeTicketId = UUID.randomUUID();
        when(deduplicationService.tryClaim(disputeTicketId.toString(), "DisputeTicketNotificationListener")).thenReturn(false);
        DisputeTicketCreatedEvent event = new DisputeTicketCreatedEvent(
            disputeTicketId, UUID.randomUUID(), UUID.randomUUID(), "Reason");

        listener.on(event);

        verifyNoInteractions(dispatcher);
    }
}
