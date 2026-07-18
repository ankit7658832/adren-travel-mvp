package com.adren.travel.ai.internal;

import com.adren.travel.ai.AiApi;
import com.adren.travel.ai.AiAuditLogEntryView;
import com.adren.travel.shared.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * HTTP surface for the AI Governance/Audit Log viewer (PRD §6, §21.6,
 * AI-11) — thin, all authorization/logic lives behind {@link AiApi}.
 * {@link PageResponse} per RULES.md §3.4's stable collection-endpoint
 * shape, matching {@code BookingQueryController.findByConsultant}.
 */
@RestController
@RequestMapping("/api/v1/ai")
class AiGovernanceController {

    private final AiApi aiApi;

    AiGovernanceController(AiApi aiApi) {
        this.aiApi = aiApi;
    }

    @GetMapping("/audit-log")
    PageResponse<AiAuditLogEntryView> findAuditLog(@RequestParam(required = false) UUID consultantId, Pageable pageable) {
        return PageResponse.of(aiApi.findAuditLog(consultantId, pageable));
    }
}
