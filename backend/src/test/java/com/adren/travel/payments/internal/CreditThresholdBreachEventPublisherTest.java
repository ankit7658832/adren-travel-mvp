package com.adren.travel.payments.internal;

import com.adren.travel.payments.event.CreditThresholdBreachedEvent;
import com.adren.travel.shared.CurrencyCode;
import com.adren.travel.shared.Money;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

/** HRD-02 — proves the publisher forwards the event as-is; the REQUIRES_NEW transaction boundary is proven at the integrationTest tier. */
@ExtendWith(MockitoExtension.class)
class CreditThresholdBreachEventPublisherTest {

    @Mock
    ApplicationEventPublisher events;

    @Test
    void publishesTheEventWithTheBookingConsultantAndAttemptedAmount() {
        CreditThresholdBreachEventPublisher publisher = new CreditThresholdBreachEventPublisher(events);
        UUID bookingId = UUID.randomUUID();
        UUID consultantId = UUID.randomUUID();
        Money amount = new Money(BigDecimal.valueOf(1_000), CurrencyCode.INR);

        publisher.publish(bookingId, consultantId, amount);

        ArgumentCaptor<CreditThresholdBreachedEvent> captor = ArgumentCaptor.forClass(CreditThresholdBreachedEvent.class);
        verify(events).publishEvent(captor.capture());
        assertThat(captor.getValue().bookingId()).isEqualTo(bookingId);
        assertThat(captor.getValue().consultantId()).isEqualTo(consultantId);
        assertThat(captor.getValue().attemptedAmount()).isEqualTo(amount);
    }
}
