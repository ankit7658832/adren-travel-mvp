import { useMutation, useQuery } from "@tanstack/react-query";
import { apiClient } from "@/shared/api/apiClient";

/**
 * The `{consultantId}` query param the backend's `/packages` endpoint
 * expects is never actually used server-side to scope the request — the
 * backend always resolves the real tenant from the caller's own JWT
 * (RULES.md §5.2), same "path segment is a URL-shape artifact only"
 * reasoning as {@code useByosCredentialEntry.ts}'s identical placeholder.
 */
const PATH_PLACEHOLDER_CONSULTANT_ID = "00000000-0000-0000-0000-000000000000";

export interface PublishedPackage {
  packageId: string;
  name: string;
  currency: string;
}

interface PackagesPageResponse {
  content: PublishedPackage[];
}

/** ADS-03, PRD §14.2 step 1 — the Campaign Builder's package selector, sourced from BOK-12's real published-packages list. */
export function usePublishedPackages() {
  return useQuery({
    queryKey: ["published-packages"],
    queryFn: async () => {
      const { data } = await apiClient.get<PackagesPageResponse>("/packages", {
        params: { consultantId: PATH_PLACEHOLDER_CONSULTANT_ID },
      });
      return data.content;
    },
  });
}

export interface AdCampaign {
  campaignId: string;
  packageId: string;
  consultantId: string;
  status: string;
  audienceDescription: string | null;
  budgetCapAmount: number | null;
  budgetCapCurrency: string;
  durationDays: number | null;
}

/** ADS-02 — creates the campaign (PendingApproval) the input form then attaches to. */
export function useCreateCampaign() {
  return useMutation({
    mutationFn: async (packageId: string) => {
      const { data } = await apiClient.post<AdCampaign>("/campaigns", { packageId });
      return data;
    },
  });
}

export interface CampaignInputsInput {
  campaignId: string;
  audienceDescription: string;
  budgetCapAmount: number;
  durationDays: number;
}

/** ADS-03, PRD §14.2 steps 1-2. */
export function useSubmitCampaignInputs() {
  return useMutation({
    mutationFn: async (input: CampaignInputsInput) => {
      const { data } = await apiClient.post<AdCampaign>(`/campaigns/${input.campaignId}/inputs`, {
        audienceDescription: input.audienceDescription,
        budgetCapAmount: input.budgetCapAmount,
        durationDays: input.durationDays,
      });
      return data;
    },
  });
}
