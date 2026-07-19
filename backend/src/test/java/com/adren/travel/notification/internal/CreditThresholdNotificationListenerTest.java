package com.adren.travel.notification.internal;

import com.adren.travel.payments.event.CreditThresholdBreachedEvent;
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

/** HRD-02 — "credit threshold" trigger dispatches on {@link CreditThresholdBreachedEvent}. */
@ExtendWith(MockitoExtension.class)
class CreditThresholdNotificationListenerTest {

    @Mock
    NotificationDispatcher dispatcher;

    @Test
    void dispatchesOnCreditThresholdBreached() {
        CreditThresholdNotificationListener listener = new CreditThresholdNotificationListener(dispatcher);
        UUID consultantId = UUID.randomUUID();
        CreditThresholdBreachedEvent event = new CreditThresholdBreachedEvent(
            consultantId, new Money(BigDecimal.valueOf(1000), CurrencyCode.INR));

        listener.on(event);

        verify(dispatcher).dispatch(eq(consultantId), any(), any(), any());
    }
}
