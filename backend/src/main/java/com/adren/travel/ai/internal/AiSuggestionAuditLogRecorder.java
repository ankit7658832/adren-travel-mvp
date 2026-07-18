package com.adren.travel.ai.internal;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * AI-07's "100%-logged, no sampling" guarantee has a failure-mode gap a
 * plain {@code saveAndFlush} inside {@code generateItinerary}'s own
 * {@code @Transactional} boundary doesn't close: when the Groq call itself
 * fails, {@code generateItinerary} records the failure via {@code
 * saveAndFlush} and then RETHROWS — and Spring's default rollback-on-
 * unchecked-exception behavior rolls back the WHOLE transaction, including
 * the audit row that was just flushed. The audit record is lost in exactly
 * the case it matters most (a real failure). {@code
 * PROPAGATION_REQUIRES_NEW} runs the insert in its OWN transaction that
 * commits independently — same reasoning, same shape, as {@code
 * payments.internal.WalletLedgerEntryRecorder} (FIN-10). This has to be a
 * SEPARATE bean, not a private method on {@code AiServiceImpl}, for the
 * same self-invocation-proxy reason that recorder's own Javadoc explains.
 */
@Component
class AiSuggestionAuditLogRecorder {

    private final AiSuggestionAuditLogRepository auditLogRepository;

    AiSuggestionAuditLogRecorder(AiSuggestionAuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void record(AiSuggestionAuditLog entry) {
        auditLogRepository.saveAndFlush(entry);
    }
}
