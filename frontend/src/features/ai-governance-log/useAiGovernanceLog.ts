import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { apiClient } from "@/shared/api/apiClient";

export interface AiAuditLogEntry {
  auditLogId: string;
  correlationId: string;
  attemptNumber: number;
  consultantId: string;
  itineraryId: string;
  requestInputJson: string;
  sourceDataSnapshotJson: string;
  aiOutputJson: string | null;
  disposition: string;
  createdAt: string;
}

interface AiAuditLogPageResponse {
  content: AiAuditLogEntry[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

const PAGE_SIZE = 20;

/**
 * PRD §6's "View AI governance/audit logs (Yes, all)" / §21.6 Super Admin
 * Console, AI-11 — the company-wide AI suggestion audit trail browser,
 * paginated and optionally filtered to one Consultant. Server data, so
 * React Query. Backed by {@code GET /api/v1/ai/audit-log}, a SUPER_ADMIN-
 * only endpoint (enforced server-side — a Consultant/User's request is
 * rejected there, not just hidden client-side).
 */
export function useAiGovernanceLog() {
  const [consultantIdFilter, setConsultantIdFilterState] = useState("");
  const [page, setPage] = useState(0);

  const query = useQuery({
    queryKey: ["ai-governance-log", consultantIdFilter, page],
    queryFn: async () => {
      const { data } = await apiClient.get<AiAuditLogPageResponse>("/ai/audit-log", {
        params: {
          consultantId: consultantIdFilter || undefined,
          page,
          size: PAGE_SIZE,
        },
      });
      return data;
    },
  });

  function setConsultantIdFilter(value: string) {
    setConsultantIdFilterState(value);
    setPage(0);
  }

  return { query, consultantIdFilter, setConsultantIdFilter, page, setPage };
}
