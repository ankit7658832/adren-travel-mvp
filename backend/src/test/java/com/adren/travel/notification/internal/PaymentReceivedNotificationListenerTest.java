package com.adren.travel.notification.internal;

import com.adren.travel.payments.event.StripePaymentSucceededEvent;
import com.adren.travel.shared.CurrencyCode;
import com.adren.travel.shared.Money;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * HRD-02 — "payment received" trigger dispatches on {@link StripePaymentSucceededEvent}.
 * HRD-03 — a redelivery of the same event (same bookingReferenceId) is a no-op.
 */
@ExtendWith(MockitoExtension.class)
class PaymentReceivedNotificationListenerTest {

    @Mock
    NotificationDispatcher dispatcher;

    @Mock
    ProcessedEventDeduplicationService deduplicationService;

    @Test
    void dispatchesOnStripePaymentSucceeded() {
        PaymentReceivedNotificationListener listener = new PaymentReceivedNotificationListener(dispatcher, deduplicationService);
        UUID consultantId = UUID.randomUUID();
        UUID bookingReferenceId = UUID.randomUUID();
        when(deduplicationService.tryClaim(bookingReferenceId.toString(), "PaymentReceivedNotificationListener")).thenReturn(true);
        StripePaymentSucceededEvent event = new StripePaymentSucceededEvent(
            bookingReferenceId, consultantId, new Money(BigDecimal.valueOf(1000), CurrencyCode.INR));

        listener.on(event);

        verify(dispatcher).dispatch(eq(consultantId), any(), any(), any());
    }

    @Test
    void aRedeliveredEventIsANoOpHRD03() {
        PaymentReceivedNotificationListener listener = new PaymentReceivedNotificationListener(dispatcher, deduplicationService);
        UUID bookingReferenceId = UUID.randomUUID();
        when(deduplicationService.tryClaim(bookingReferenceId.toString(), "PaymentReceivedNotificationListener")).thenReturn(false);
        StripePaymentSucceededEvent event = new StripePaymentSucceededEvent(
            bookingReferenceId, UUID.randomUUID(), new Money(BigDecimal.valueOf(1000), CurrencyCode.INR));

        listener.on(event);

        verifyNoInteractions(dispatcher);
    }
}
