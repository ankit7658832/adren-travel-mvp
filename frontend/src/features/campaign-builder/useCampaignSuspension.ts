import { useQuery } from "@tanstack/react-query";
import { apiClient } from "@/shared/api/apiClient";

interface CampaignSuspensionResponse {
  campaignId: string;
  metaSuspended: boolean;
}

/** ADS-13, PRD §23.5 Edge Case #12 / §25 T17 — a single campaign's mocked Meta ad-account suspension status. */
export function useCampaignSuspension(campaignId: string) {
  return useQuery({
    queryKey: ["campaign-suspension", campaignId],
    queryFn: async () => {
      const { data } = await apiClient.get<CampaignSuspensionResponse>(`/campaigns/${campaignId}`);
      return Boolean(data.metaSuspended);
    },
    enabled: Boolean(campaignId),
  });
}
