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

export interface AdCreativeVariantDto {
  headline: string;
  bodyText: string;
}

/**
 * Mirrors the backend's {@code AdCreativeGenerationResult} sealed
 * interface (AI-05's explicit-failure-state principle) — "no viable
 * creative" is a legitimate, well-typed outcome the gallery renders as an
 * empty state, never an error.
 */
export type AdCreativeGenerationResult =
  | { type: "AD_CREATIVE_SUGGESTION"; auditLogId: string; variants: AdCreativeVariantDto[] }
  | { type: "NO_VIABLE_AD_CREATIVE"; auditLogId: string; reason: string };

/** ADS-04, PRD §14.2 step 3. */
export function useGenerateCreativeVariants() {
  return useMutation({
    mutationFn: async (input: { campaignId: string; variantCount: number }) => {
      const { data } = await apiClient.post<AdCreativeGenerationResult>(
        `/campaigns/${input.campaignId}/creative-variants`,
        { variantCount: input.variantCount }
      );
      return data;
    },
  });
}

export interface PersistedCreativeVariant {
  variantId: string;
  campaignId: string;
  headline: string;
  bodyText: string;
  imageRef: string | null;
  approved: boolean;
}

/** ADS-04 — re-fetches whatever variants a prior generation call already persisted, e.g. after a page reload. */
export function useCreativeVariants(campaignId: string | null) {
  return useQuery({
    queryKey: ["campaign-creative-variants", campaignId],
    queryFn: async () => {
      const { data } = await apiClient.get<PersistedCreativeVariant[]>(`/campaigns/${campaignId}/creative-variants`);
      return data;
    },
    enabled: campaignId !== null,
  });
}
