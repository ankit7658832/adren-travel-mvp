import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { apiClient } from "@/shared/api/apiClient";

export interface PendingReviewCampaign {
  campaignId: string;
  packageId: string;
  consultantId: string;
  status: string;
  audienceDescription: string | null;
  budgetCapAmount: number | null;
  budgetCapCurrency: string;
  durationDays: number | null;
}

interface PendingReviewPageResponse {
  content: PendingReviewCampaign[];
}

const QUEUE_QUERY_KEY = ["campaigns-pending-policy-review"];

/** ADS-06, PRD §14.2 step 5 — the Super Admin Console's brand-safety/policy review queue. */
export function useCampaignsPendingPolicyReview() {
  return useQuery({
    queryKey: QUEUE_QUERY_KEY,
    queryFn: async () => {
      const { data } = await apiClient.get<PendingReviewPageResponse>("/campaigns/pending-policy-review");
      return data.content;
    },
  });
}

/** ADS-06 — rejecting removes a campaign from the queue (it moves to Rejected); approval has no separate action here since launching (ADS-07) is itself the approval step. */
export function useRejectCampaignPolicyReview() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (input: { campaignId: string; reason: string }) => {
      const { data } = await apiClient.post(`/campaigns/${input.campaignId}/policy-review`, { reason: input.reason });
      return data;
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: QUEUE_QUERY_KEY }),
  });
}
