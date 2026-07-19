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

/** HRD-02 — "payment received" trigger dispatches on {@link StripePaymentSucceededEvent}. */
@ExtendWith(MockitoExtension.class)
class PaymentReceivedNotificationListenerTest {

    @Mock
    NotificationDispatcher dispatcher;

    @Test
    void dispatchesOnStripePaymentSucceeded() {
        PaymentReceivedNotificationListener listener = new PaymentReceivedNotificationListener(dispatcher);
        UUID consultantId = UUID.randomUUID();
        StripePaymentSucceededEvent event = new StripePaymentSucceededEvent(
            UUID.randomUUID(), consultantId, new Money(BigDecimal.valueOf(1000), CurrencyCode.INR));

        listener.on(event);

        verify(dispatcher).dispatch(eq(consultantId), any(), any(), any());
    }
}
