import { useQuery } from "@tanstack/react-query";
import { apiClient } from "@/shared/api/apiClient";

/**
 * The `consultantId` query param the backend's `GET /campaigns` endpoint
 * expects is never actually used server-side to scope the request — the
 * backend always resolves the real tenant from the caller's own JWT
 * (RULES.md §5.2), same "path segment is a URL-shape artifact only"
 * reasoning as {@code useCampaignBuilder.ts}'s identical placeholder.
 */
const PATH_PLACEHOLDER_CONSULTANT_ID = "00000000-0000-0000-0000-000000000000";

export interface ActiveCampaign {
  campaignId: string;
  packageId: string;
  consultantId: string;
  status: string;
  audienceDescription: string | null;
  budgetCapAmount: number | null;
  budgetCapCurrency: string;
  durationDays: number | null;
  metaCampaignRef: string | null;
  spendToDateAmount: number | null;
  rejectionReason: string | null;
  impressions: number;
  clicks: number;
  bookingsAttributed: number;
}

interface ActiveCampaignsPageResponse {
  content: ActiveCampaign[];
}

/** ADS-09, PRD §14.2 step 7 — the Consultant Dashboard's Active Campaigns tab (§21.5, HRD-09 composes this hook's data into the full dashboard). */
export function useActiveCampaigns() {
  return useQuery({
    queryKey: ["active-campaigns"],
    queryFn: async () => {
      const { data } = await apiClient.get<ActiveCampaignsPageResponse>("/campaigns", {
        params: { consultantId: PATH_PLACEHOLDER_CONSULTANT_ID },
      });
      return data.content;
    },
  });
}
