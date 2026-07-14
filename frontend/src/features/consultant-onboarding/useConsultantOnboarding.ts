import { useMutation, useQuery } from "@tanstack/react-query";
import { apiClient } from "@/shared/api/apiClient";

export type Market = "INDIA" | "AUSTRALIA" | "UK" | "USA" | "DUBAI_UAE" | "DENMARK";

export const MARKETS: { value: Market; label: string }[] = [
  { value: "INDIA", label: "India" },
  { value: "AUSTRALIA", label: "Australia" },
  { value: "UK", label: "United Kingdom" },
  { value: "USA", label: "United States" },
  { value: "DUBAI_UAE", label: "Dubai/UAE" },
  { value: "DENMARK", label: "Denmark" },
];

export interface KycFieldDefinition {
  fieldKey: string;
  label: string;
  required: boolean;
}

export interface OnboardConsultantInput {
  businessName: string;
  homeMarket: Market;
  kycFields: Record<string, string>;
}

/**
 * Data-driven per-market KYC field set (RULES.md §24.7 / FND-04) — fetched
 * from the backend's rule table rather than a hardcoded frontend map, so a
 * market-rule change never requires a frontend deploy that can drift out
 * of sync (the same principle FES-09 later generalizes for other forms).
 */
export function useKycRules(market: Market | null) {
  return useQuery({
    queryKey: ["consultant-kyc-rules", market],
    queryFn: async () => {
      const { data } = await apiClient.get<KycFieldDefinition[]>("/consultants/kyc-rules", {
        params: { market },
      });
      return data;
    },
    enabled: market !== null,
  });
}

export function useOnboardConsultant() {
  return useMutation({
    mutationFn: async (input: OnboardConsultantInput) => {
      const { data } = await apiClient.post<{ consultantId: string }>("/consultants", input);
      return data;
    },
  });
}
