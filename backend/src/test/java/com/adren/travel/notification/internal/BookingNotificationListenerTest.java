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

/**
 * HRD-01 — proves the listener dispatches on {@code BookingConfirmedEvent}
 * with the right consultant/subject/message; the region-routing logic
 * itself is {@link NotificationDispatcher}'s own concern, tested there.
 */
@ExtendWith(MockitoExtension.class)
class BookingNotificationListenerTest {

    @Mock
    NotificationDispatcher dispatcher;

    BookingNotificationListener listener;

    @Test
    void dispatchesOnBookingConfirmedWithTheBookingIdInTheMessage() {
        listener = new BookingNotificationListener(dispatcher);
        UUID consultantId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        BookingConfirmedEvent event = new BookingConfirmedEvent(
            bookingId, consultantId, new Money(BigDecimal.valueOf(1000), CurrencyCode.INR));

        listener.on(event);

        verify(dispatcher).dispatch(eq(consultantId), any(), contains(bookingId.toString()), contains(bookingId.toString()));
    }
}
