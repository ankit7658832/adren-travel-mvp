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

/**
 * FIN-16's dispute-ticket AC: the listener dispatches with the right
 * consultant/message; region-routing is {@link NotificationDispatcher}'s
 * own concern, tested there (same shape as {@link BookingNotificationListenerTest}).
 */
@ExtendWith(MockitoExtension.class)
class DisputeTicketNotificationListenerTest {

    @Mock
    NotificationDispatcher dispatcher;

    DisputeTicketNotificationListener listener;

    @Test
    void dispatchesOnDisputeTicketCreatedWithTheReasonInTheEmailBody() {
        listener = new DisputeTicketNotificationListener(dispatcher);
        UUID consultantId = UUID.randomUUID();
        DisputeTicketCreatedEvent event = new DisputeTicketCreatedEvent(
            UUID.randomUUID(), UUID.randomUUID(), consultantId, "Wrong room type delivered");

        listener.on(event);

        verify(dispatcher).dispatch(eq(consultantId), any(), contains("Wrong room type delivered"), any());
    }
}
