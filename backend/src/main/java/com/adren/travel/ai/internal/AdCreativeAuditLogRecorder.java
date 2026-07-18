package com.adren.travel.ai.internal;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Same {@code REQUIRES_NEW} shape as {@link AiSuggestionAuditLogRecorder}
 * (see that class's Javadoc for the full reasoning) — a GROQ_ERROR row for
 * a failed ad-creative generation attempt must survive even though {@code
 * generateAdCreative} itself rethrows afterward. Kept as a distinct bean
 * (not a reused/generic recorder) since it writes a different entity type
 * to a different table.
 */
@Component
class AdCreativeAuditLogRecorder {

    private final AdCreativeAuditLogRepository auditLogRepository;

    AdCreativeAuditLogRecorder(AdCreativeAuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void record(AdCreativeAuditLog entry) {
        auditLogRepository.saveAndFlush(entry);
    }
}
