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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * HRD-02 — "credit threshold" trigger dispatches on {@link CreditThresholdBreachedEvent}.
 * HRD-03 — a redelivery of the same event (same bookingId) is a no-op.
 */
@ExtendWith(MockitoExtension.class)
class CreditThresholdNotificationListenerTest {

    @Mock
    NotificationDispatcher dispatcher;

    @Mock
    ProcessedEventDeduplicationService deduplicationService;

    @Test
    void dispatchesOnCreditThresholdBreached() {
        CreditThresholdNotificationListener listener = new CreditThresholdNotificationListener(dispatcher, deduplicationService);
        UUID bookingId = UUID.randomUUID();
        UUID consultantId = UUID.randomUUID();
        when(deduplicationService.tryClaim(bookingId.toString(), "CreditThresholdNotificationListener")).thenReturn(true);
        CreditThresholdBreachedEvent event = new CreditThresholdBreachedEvent(
            bookingId, consultantId, new Money(BigDecimal.valueOf(1000), CurrencyCode.INR));

        listener.on(event);

        verify(dispatcher).dispatch(eq(consultantId), any(), any(), any());
    }

    @Test
    void aRedeliveredEventIsANoOpHRD03() {
        CreditThresholdNotificationListener listener = new CreditThresholdNotificationListener(dispatcher, deduplicationService);
        UUID bookingId = UUID.randomUUID();
        when(deduplicationService.tryClaim(bookingId.toString(), "CreditThresholdNotificationListener")).thenReturn(false);
        CreditThresholdBreachedEvent event = new CreditThresholdBreachedEvent(
            bookingId, UUID.randomUUID(), new Money(BigDecimal.valueOf(1000), CurrencyCode.INR));

        listener.on(event);

        verifyNoInteractions(dispatcher);
    }
}
