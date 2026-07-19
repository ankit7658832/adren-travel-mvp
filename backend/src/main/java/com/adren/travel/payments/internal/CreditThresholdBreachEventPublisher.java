package com.adren.travel.payments.internal;

import com.adren.travel.payments.event.CreditThresholdBreachedEvent;
import com.adren.travel.shared.Money;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * HRD-02 — publishes {@link CreditThresholdBreachedEvent} in its OWN {@code
 * REQUIRES_NEW} transaction, same shape and reason as {@link WalletLedgerEntryRecorder}:
 * {@code placeHold} throws {@link com.adren.travel.payments.CreditLimitExceededException}
 * and rolls back the CALLING transaction, which would also roll back (and
 * so silently drop) an event published in that same transaction — Spring
 * Modulith's event-publication registry write is itself transactional.
 * Running in a separate, already-committed transaction means the
 * notification fires even though the wallet-hold write itself never
 * happened, which is exactly the desired behavior: the block worked
 * correctly, and the Consultant still needs to hear about it.
 * <p>
 * Separate bean for the same self-invocation reason {@link WalletLedgerEntryRecorder}
 * documents — {@code @Transactional} propagation only takes effect through
 * the Spring AOP proxy.
 */
@Component
class CreditThresholdBreachEventPublisher {

    private final ApplicationEventPublisher events;

    CreditThresholdBreachEventPublisher(ApplicationEventPublisher events) {
        this.events = events;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void publish(UUID consultantId, Money attemptedAmount) {
        events.publishEvent(new CreditThresholdBreachedEvent(consultantId, attemptedAmount));
    }
}
