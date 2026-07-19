import { Badge } from "@/shared/design-system/Badge";
import { Button } from "@/shared/design-system/Button";
import { useActiveCampaigns, type ActiveCampaign } from "./useActiveCampaigns";

/**
 * PRD §14.2 step 7, §21.5 — the Consultant Dashboard's Active Campaigns
 * tab (ADS-09): every one of the Consultant's own campaigns with its
 * current {@code performance_snapshot} (impressions/clicks/bookings
 * attributed, PRD §20.13). Built here as a standalone, composable piece —
 * HRD-09's {@code ConsultantDashboard} imports this component directly
 * rather than duplicating the fetch, matching how {@code CampaignBuilder}
 * composes {@code CreativeVariantGallery}/{@code CreativeApprovalPanel}.
 *
 * States implemented: loading, error, empty (no campaigns yet), success.
 */
export function ActiveCampaignsTab() {
  const campaignsQuery = useActiveCampaigns();

  if (campaignsQuery.isLoading) {
    return (
      <p role="status" className="text-sm text-neutral-600">
        Loading your campaigns…
      </p>
    );
  }

  if (campaignsQuery.isError) {
    return (
      <div role="alert" className="flex items-center justify-between rounded-md border border-error-600/20 bg-error-50 px-4 py-3">
        <p className="text-sm text-error-700">Could not load your campaigns.</p>
        <Button variant="secondary" size="sm" onClick={() => campaignsQuery.refetch()}>
          Retry
        </Button>
      </div>
    );
  }

  const campaigns = campaignsQuery.data ?? [];

  if (campaigns.length === 0) {
    return <p className="text-sm text-neutral-600">You have no campaigns yet — promote a Package to get started.</p>;
  }

  return (
    <ul aria-label="active-campaigns" className="space-y-3">
      {campaigns.map((campaign) => (
        <ActiveCampaignRow key={campaign.campaignId} campaign={campaign} />
      ))}
    </ul>
  );
}

function ActiveCampaignRow({ campaign }: { campaign: ActiveCampaign }) {
  return (
    <li className="rounded-md border border-neutral-200 bg-surface px-4 py-3">
      <div className="flex items-center justify-between">
        <p className="text-base font-medium text-neutral-900">{campaign.audienceDescription ?? "Untitled campaign"}</p>
        <Badge tone={campaign.status === "LIVE" ? "success" : "neutral"}>{campaign.status}</Badge>
      </div>
      <p className="mt-2 text-sm text-neutral-600">
        {campaign.impressions.toLocaleString()} impressions · {campaign.clicks.toLocaleString()} clicks ·{" "}
        {campaign.bookingsAttributed.toLocaleString()} bookings attributed
      </p>
    </li>
  );
}
