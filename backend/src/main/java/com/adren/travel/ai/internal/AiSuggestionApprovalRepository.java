package com.adren.travel.ai.internal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface AiSuggestionApprovalRepository extends JpaRepository<AiSuggestionApproval, UUID> {

    List<AiSuggestionApproval> findByAuditLogId(UUID auditLogId);
}
