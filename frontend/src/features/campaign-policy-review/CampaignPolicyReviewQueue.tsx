import { useState } from "react";
import { Button } from "@/shared/design-system/Button";
import { TextField } from "@/shared/design-system/TextField";
import {
  useCampaignsPendingPolicyReview,
  useRejectCampaignPolicyReview,
  type PendingReviewCampaign,
} from "./useCampaignPolicyReview";

/**
 * PRD §14.2 step 5, §21.6 — the Super Admin Console's brand-safety/policy
 * review queue (ADS-06): every campaign a Consultant has submitted (all
 * creative variants approved) and not yet decided. Rejecting requires a
 * reason, surfaced back to the Consultant (this story's own AC #2);
 * approving has no separate action here — launching the campaign (ADS-07)
 * is itself the approval step, since PRD §20.13's status enum has no
 * distinct "approved, not yet live" state.
 *
 * States implemented: loading (fetching the queue), error (fetch failed),
 * empty (nothing pending review), success (the queue, with a per-row
 * reject action).
 */
export function CampaignPolicyReviewQueue() {
  const queueQuery = useCampaignsPendingPolicyReview();

  if (queueQuery.isLoading) {
    return (
      <main className="mx-auto max-w-3xl px-6 py-8">
        <h1 className="text-2xl font-semibold text-neutral-900">Campaign Policy Review</h1>
        <p role="status" className="mt-4 text-sm text-neutral-600">
          Loading the review queue…
        </p>
      </main>
    );
  }

  if (queueQuery.isError) {
    return (
      <main className="mx-auto max-w-3xl px-6 py-8">
        <h1 className="text-2xl font-semibold text-neutral-900">Campaign Policy Review</h1>
        <div role="alert" className="mt-4 flex items-center justify-between rounded-md border border-error-600/20 bg-error-50 px-4 py-3">
          <p className="text-sm text-error-700">Could not load the review queue.</p>
          <Button variant="secondary" size="sm" onClick={() => queueQuery.refetch()}>
            Retry
          </Button>
        </div>
      </main>
    );
  }

  const campaigns = queueQuery.data ?? [];

  return (
    <main className="mx-auto max-w-3xl px-6 py-8">
      <h1 className="text-2xl font-semibold text-neutral-900">Campaign Policy Review</h1>

      {campaigns.length === 0 ? (
        <p className="mt-4 text-sm text-neutral-600">No campaigns are currently pending policy review.</p>
      ) : (
        <ul aria-label="policy-review-queue" className="mt-6 space-y-3">
          {campaigns.map((campaign) => (
            <CampaignReviewRow key={campaign.campaignId} campaign={campaign} />
          ))}
        </ul>
      )}
    </main>
  );
}

function CampaignReviewRow({ campaign }: { campaign: PendingReviewCampaign }) {
  const [reason, setReason] = useState("");
  const reject = useRejectCampaignPolicyReview();

  return (
    <li className="rounded-md border border-neutral-200 bg-surface px-4 py-3">
      <p className="text-base font-medium text-neutral-900">{campaign.audienceDescription ?? "Untitled campaign"}</p>
      <p className="mt-1 text-sm text-neutral-600">
        Budget {campaign.budgetCapAmount} {campaign.budgetCapCurrency} · {campaign.durationDays} days
      </p>

      <div className="mt-3 flex items-end gap-3">
        <div className="flex-1">
          <TextField
            label="Rejection reason"
            value={reason}
            onChange={(e) => setReason(e.target.value)}
          />
        </div>
        <Button
          variant="destructive"
          disabled={!reason.trim() || reject.isPending}
          onClick={() => reject.mutate({ campaignId: campaign.campaignId, reason })}
        >
          {reject.isPending ? "Rejecting…" : "Reject"}
        </Button>
      </div>

      {reject.isError && (
        <p role="alert" className="mt-2 text-sm text-error-700">
          Could not reject this campaign. Please try again.
        </p>
      )}
    </li>
  );
}
