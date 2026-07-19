package com.adren.travel.notification.internal;

import com.adren.travel.booking.event.BookingCancelledEvent;
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
 * HRD-05 — notification dispatch on refund completion. HRD-03 — a
 * redelivery of the same event (same bookingId) is a no-op.
 */
@ExtendWith(MockitoExtension.class)
class BookingCancelledNotificationListenerTest {

    @Mock
    NotificationDispatcher dispatcher;

    @Mock
    ProcessedEventDeduplicationService deduplicationService;

    @Test
    void dispatchesOnBookingCancelledWithTheRefundAmountInTheMessage() {
        BookingCancelledNotificationListener listener = new BookingCancelledNotificationListener(dispatcher, deduplicationService);
        UUID bookingId = UUID.randomUUID();
        UUID consultantId = UUID.randomUUID();
        when(deduplicationService.tryClaim(bookingId.toString(), "BookingCancelledNotificationListener")).thenReturn(true);
        BookingCancelledEvent event = new BookingCancelledEvent(bookingId, consultantId,
            new Money(BigDecimal.valueOf(9500), CurrencyCode.INR), Money.zero(CurrencyCode.INR));

        listener.on(event);

        verify(dispatcher).dispatch(eq(consultantId), any(), contains("9500"), contains(bookingId.toString()));
    }

    @Test
    void aRedeliveredEventIsANoOpHRD03() {
        BookingCancelledNotificationListener listener = new BookingCancelledNotificationListener(dispatcher, deduplicationService);
        UUID bookingId = UUID.randomUUID();
        when(deduplicationService.tryClaim(bookingId.toString(), "BookingCancelledNotificationListener")).thenReturn(false);
        BookingCancelledEvent event = new BookingCancelledEvent(bookingId, UUID.randomUUID(),
            new Money(BigDecimal.valueOf(9500), CurrencyCode.INR), Money.zero(CurrencyCode.INR));

        listener.on(event);

        verifyNoInteractions(dispatcher);
    }
}
