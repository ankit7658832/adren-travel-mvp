package com.adren.travel.notification.internal;

import com.adren.travel.booking.event.BookingConfirmedEvent;
import com.adren.travel.shared.CurrencyCode;
import com.adren.travel.shared.Money;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * HRD-01 — proves the listener dispatches on {@code BookingConfirmedEvent}
 * with the right consultant/subject/message; the region-routing logic
 * itself is {@link NotificationDispatcher}'s own concern, tested there.
 * HRD-03 — a redelivery of the same event (same bookingId) is a no-op.
 */
@ExtendWith(MockitoExtension.class)
class BookingNotificationListenerTest {

    @Mock
    NotificationDispatcher dispatcher;

    @Mock
    ProcessedEventDeduplicationService deduplicationService;

    BookingNotificationListener listener;

    @Test
    void dispatchesOnBookingConfirmedWithTheBookingIdInTheMessage() {
        listener = new BookingNotificationListener(dispatcher, deduplicationService);
        UUID consultantId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        when(deduplicationService.tryClaim(bookingId.toString(), "BookingNotificationListener")).thenReturn(true);
        BookingConfirmedEvent event = new BookingConfirmedEvent(
            bookingId, consultantId, new Money(BigDecimal.valueOf(1000), CurrencyCode.INR));

        listener.on(event);

        verify(dispatcher).dispatch(eq(consultantId), any(), contains(bookingId.toString()), contains(bookingId.toString()));
    }

    @Test
    void aRedeliveredEventIsANoOpFIN10HRD03() {
        listener = new BookingNotificationListener(dispatcher, deduplicationService);
        UUID bookingId = UUID.randomUUID();
        when(deduplicationService.tryClaim(bookingId.toString(), "BookingNotificationListener")).thenReturn(false);
        BookingConfirmedEvent event = new BookingConfirmedEvent(
            bookingId, UUID.randomUUID(), new Money(BigDecimal.valueOf(1000), CurrencyCode.INR));

        listener.on(event);

        verifyNoInteractions(dispatcher);
    }
}
