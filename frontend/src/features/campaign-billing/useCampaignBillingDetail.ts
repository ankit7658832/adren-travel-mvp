import { useQuery } from "@tanstack/react-query";
import { apiClient } from "@/shared/api/apiClient";

export interface SpendTransaction {
  transactionId: string;
  amount: number;
  recordedAt: string;
}

export interface CampaignBillingDetail {
  campaignId: string;
  spendToDateAmount: number;
  budgetCapAmount: number | null;
  budgetCapCurrency: string;
  transactions: SpendTransaction[];
}

/** ADS-11, PRD §14.3 — the billing-transparency detail view for a single campaign. */
export function useCampaignBillingDetail(campaignId: string) {
  return useQuery({
    queryKey: ["campaign-billing-detail", campaignId],
    queryFn: async () => {
      const { data } = await apiClient.get<CampaignBillingDetail>(`/campaigns/${campaignId}/billing-detail`);
      return data;
    },
    enabled: Boolean(campaignId),
  });
}
