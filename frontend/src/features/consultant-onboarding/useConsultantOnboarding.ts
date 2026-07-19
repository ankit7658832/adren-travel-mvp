import { useMutation } from "@tanstack/react-query";
import { apiClient } from "@/shared/api/apiClient";
import { useMarketDependentFields, type SchemaField } from "@/shared/forms/useMarketDependentFields";

export type Market = "INDIA" | "AUSTRALIA" | "UK" | "USA" | "DUBAI_UAE" | "DENMARK";

export const MARKETS: { value: Market; label: string }[] = [
  { value: "INDIA", label: "India" },
  { value: "AUSTRALIA", label: "Australia" },
  { value: "UK", label: "United Kingdom" },
  { value: "USA", label: "United States" },
  { value: "DUBAI_UAE", label: "Dubai/UAE" },
  { value: "DENMARK", label: "Denmark" },
];

export type KycFieldDefinition = SchemaField;

export interface OnboardConsultantInput {
  businessName: string;
  homeMarket: Market;
  kycFields: Record<string, string>;
}

/**
 * FES-09 — thin wrapper over the shared, generic field-resolution engine
 * (`useMarketDependentFields`), pointed at FND-04's specific endpoint.
 * RULES.md §24.7's data-driven KYC principle: the market→fields mapping
 * lives entirely on the backend, never duplicated as a frontend map.
 */
export function useKycRules(market: Market | null) {
  return useMarketDependentFields("/consultants/kyc-rules", market);
}

export function useOnboardConsultant() {
  return useMutation({
    mutationFn: async (input: OnboardConsultantInput) => {
      const { data } = await apiClient.post<{ consultantId: string }>("/consultants", input);
      return data;
    },
  });
}
