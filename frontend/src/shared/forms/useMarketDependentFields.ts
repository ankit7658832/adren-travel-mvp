import { useQuery } from "@tanstack/react-query";
import { apiClient } from "@/shared/api/apiClient";

export interface SchemaField {
  fieldKey: string;
  label: string;
  required: boolean;
}

/**
 * FES-09 — the schema-driven field-resolution engine: fetches a
 * market-dependent required-field set from a backend rule table (PRD
 * §24.7's data-driven KYC principle) instead of a hardcoded per-market
 * conditional. Generic over the endpoint so any future market-dependent
 * form (not just FND-04's Consultant onboarding wizard) can reuse it —
 * the market→fields mapping always lives on the backend, never
 * duplicated frontend-side, so a rule-table change takes effect without a
 * frontend deploy.
 */
export function useMarketDependentFields(endpoint: string, market: string | null) {
  return useQuery({
    queryKey: [endpoint, "market-fields", market],
    queryFn: async () => {
      const { data } = await apiClient.get<SchemaField[]>(endpoint, { params: { market } });
      return data;
    },
    enabled: market !== null,
  });
}
