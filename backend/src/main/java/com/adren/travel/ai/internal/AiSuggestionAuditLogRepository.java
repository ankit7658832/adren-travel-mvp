package com.adren.travel.ai.internal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface AiSuggestionAuditLogRepository extends JpaRepository<AiSuggestionAuditLog, UUID> {

    List<AiSuggestionAuditLog> findByCorrelationIdOrderByAttemptNumber(UUID correlationId);

    List<AiSuggestionAuditLog> findByItineraryIdOrderByCreatedAt(UUID itineraryId);
}
