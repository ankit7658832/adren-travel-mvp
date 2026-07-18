import { useMutation } from "@tanstack/react-query";
import { apiClient } from "@/shared/api/apiClient";

export interface AiSuggestedLineItemDto {
  supplierId: string;
  supplierRateId: string;
  propertyName: string;
  roomType: string;
  netRate: { amount: string; currency: string };
  availabilityAsOf: string;
}

export type AiAssistResult =
  | { type: "SUGGESTION"; auditLogId: string; lineItems: AiSuggestedLineItemDto[] }
  | { type: "NO_VIABLE_SUGGESTION"; auditLogId: string; reason: string };

export interface CompleteWithAiInput {
  locationCode: string;
  checkIn: string; // ISO date (yyyy-MM-dd)
  checkOut: string; // ISO date (yyyy-MM-dd)
  naturalLanguageRequest: string;
  budgetAmount?: string;
  budgetCurrency?: string;
}

/**
 * PRD §21.2/§11.2, AI-03/AI-10 — the "Complete with AI" entry point's data
 * source. Both calls are mutations, not queries: generation and approval
 * are explicit, Consultant-triggered actions with real side effects (a
 * Groq call, an audit-log write) — never something that should silently
 * re-fetch/refire the way a GET query would (RULES.md §7.1).
 * {@code generate} calls the AI-03 "complete" endpoint (not AI-02's
 * "generate fresh" one) so an itinerary that already has a hotel selection
 * has that selection respected rather than replaced.
 */
export function useAiAssist(itineraryId: string) {
  const generate = useMutation({
    mutationFn: async (input: CompleteWithAiInput) => {
      const { data } = await apiClient.post<AiAssistResult>(`/itineraries/${itineraryId}/ai-completion`, input);
      return data;
    },
  });

  const approve = useMutation({
    mutationFn: async (params: { auditLogId: string; finalLineItems: AiSuggestedLineItemDto[] }) => {
      await apiClient.post(`/itineraries/${itineraryId}/ai-suggestion/approval`, params);
    },
  });

  return { generate, approve };
}
