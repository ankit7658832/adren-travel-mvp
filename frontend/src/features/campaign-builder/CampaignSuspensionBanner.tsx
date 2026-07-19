import { Badge } from "@/shared/design-system/Badge";
import { useCampaignSuspension } from "./useCampaignSuspension";

export interface CampaignSuspensionBannerProps {
  campaignId: string;
}

/**
 * PRD §23.5 Edge Case #12 / §25 T17, ADS-13 — a mocked Meta ad-account
 * suspension is never a silent stop: this banner surfaces "suspended —
 * action required" explicitly wherever a campaign's status is shown.
 *
 * States implemented: loading (an accessible-only status, since a banner
 * shouldn't visually flash while its own data is still resolving),
 * error (fetch failed — a quiet inline notice, not alarming, since a
 * failed suspension-status check is not itself the suspension), empty
 * (not suspended — renders nothing, which is the correct behavior, not a
 * gap), success (suspended — the actual warning banner).
 */
export function CampaignSuspensionBanner({ campaignId }: CampaignSuspensionBannerProps) {
  const suspensionQuery = useCampaignSuspension(campaignId);

  if (suspensionQuery.isLoading) {
    return <span role="status" className="sr-only" />;
  }

  if (suspensionQuery.isError) {
    return (
      <p role="alert" className="mt-4 text-sm text-neutral-600">
        Could not check this campaign&apos;s Meta account status.
      </p>
    );
  }

  if (!suspensionQuery.data) {
    return null;
  }

  return (
    <div
      role="alert"
      className="mt-4 flex items-center justify-between rounded-md border border-error-600/20 bg-error-50 px-4 py-3"
    >
      <p className="text-sm text-error-700">Your Meta ad account has been suspended.</p>
      <Badge tone="error">Suspended — action required</Badge>
    </div>
  );
}
