package com.adren.travel.ai.internal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface AiSuggestionAuditLogRepository extends JpaRepository<AiSuggestionAuditLog, UUID> {

    List<AiSuggestionAuditLog> findByCorrelationIdOrderByAttemptNumber(UUID correlationId);

    List<AiSuggestionAuditLog> findByItineraryIdOrderByCreatedAt(UUID itineraryId);

    /** AI-11 — the Super Admin Console's unfiltered browse view, newest first. */
    Page<AiSuggestionAuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /** AI-11 — filtered to one Consultant, newest first. */
    Page<AiSuggestionAuditLog> findByConsultantIdOrderByCreatedAtDesc(UUID consultantId, Pageable pageable);

    /** HRD-11 — the Super Admin Dashboard's AI governance summary, one count per disposition. */
    long countByDisposition(AiSuggestionDisposition disposition);
}
