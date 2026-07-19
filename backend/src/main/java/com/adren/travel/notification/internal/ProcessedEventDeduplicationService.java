package com.adren.travel.notification.internal;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

/**
 * HRD-03 / RULES.md §2.2 — every {@code @ApplicationModuleListener} in this
 * module calls {@link #tryClaim} before dispatching. The existence check is
 * a fast-path only (avoids the exception-throwing path in the common,
 * uncontended case); the {@code save} + caught {@link DataIntegrityViolationException}
 * is the actual guarantee under a genuine concurrent/redelivered race —
 * same shape as {@code payments.internal.WalletLedgerEntryRecorder}'s FIN-10
 * idempotency pattern. No {@code REQUIRES_NEW} needed here (unlike that
 * class): a notification listener has no surrounding {@code @Transactional}
 * to isolate from — {@code @ApplicationModuleListener} methods run
 * un-transacted by default, so a plain repository call already commits in
 * its own auto-transaction.
 */
@Component
class ProcessedEventDeduplicationService {

    private final ProcessedEventRepository repository;

    ProcessedEventDeduplicationService(ProcessedEventRepository repository) {
        this.repository = repository;
    }

    /** @return true if this call claimed the pair (never processed before); false if it was already claimed. */
    boolean tryClaim(String eventId, String listenerName) {
        if (repository.existsByEventIdAndListenerName(eventId, listenerName)) {
            return false;
        }
        try {
            repository.saveAndFlush(new ProcessedEvent(eventId, listenerName));
            return true;
        } catch (DataIntegrityViolationException e) {
            return false;
        }
    }
}
