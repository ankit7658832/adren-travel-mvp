import { Button } from "@/shared/design-system/Button";
import { useApproveCreativeVariant, useCreativeVariants } from "./useCampaignBuilder";

export interface CreativeApprovalPanelProps {
  campaignId: string;
}

/**
 * PRD §14.2 step 4, §21.8 — mandatory per-variant Consultant approval
 * before a campaign can be submitted for policy review (mirroring AI-06's
 * human-in-the-loop pattern). ADS-05's own AC: attempting to proceed
 * without every variant checked is blocked, not just discouraged — the
 * readiness message below is what a later "Submit for policy review"
 * action (ADS-06) gates on.
 *
 * States implemented: loading (fetching variants), error (fetch failed),
 * empty (no variants generated yet — nothing to approve), success (the
 * approval checklist). There is no separate "default" state distinct from
 * loading/success here.
 */
export function CreativeApprovalPanel({ campaignId }: CreativeApprovalPanelProps) {
  const variantsQuery = useCreativeVariants(campaignId);
  const approve = useApproveCreativeVariant(campaignId);

  if (variantsQuery.isLoading) {
    return (
      <p role="status" className="mt-4 text-sm text-neutral-600">
        Loading creative variants…
      </p>
    );
  }

  if (variantsQuery.isError) {
    return (
      <div role="alert" className="mt-4 flex items-center justify-between rounded-md border border-error-600/20 bg-error-50 px-4 py-3">
        <p className="text-sm text-error-700">Could not load creative variants.</p>
        <Button variant="secondary" size="sm" onClick={() => variantsQuery.refetch()}>
          Retry
        </Button>
      </div>
    );
  }

  const variants = variantsQuery.data ?? [];

  if (variants.length === 0) {
    return (
      <p className="mt-4 text-sm text-neutral-600">
        Generate ad creative first — each variant must be individually approved before the campaign can be submitted
        for policy review.
      </p>
    );
  }

  const approvedCount = variants.filter((v) => v.approved).length;
  const allApproved = approvedCount === variants.length;

  return (
    <div className="mt-4">
      <h2 className="text-sm font-semibold text-neutral-900">Approve creative variants</h2>
      <ul aria-label="creative-approval-checklist" className="mt-3 space-y-2">
        {variants.map((variant) => (
          <li key={variant.variantId} className="flex items-center gap-3 rounded-md border border-neutral-200 px-3 py-2">
            <input
              id={`approve-${variant.variantId}`}
              type="checkbox"
              checked={variant.approved}
              disabled={variant.approved || approve.isPending}
              onChange={() => approve.mutate(variant.variantId)}
            />
            <label htmlFor={`approve-${variant.variantId}`} className="text-sm text-neutral-900">
              {variant.headline}
            </label>
          </li>
        ))}
      </ul>

      {approve.isError && (
        <p role="alert" className="mt-2 text-sm text-error-700">
          Could not approve this variant. Please try again.
        </p>
      )}

      <p role={allApproved ? "status" : "alert"} className={`mt-3 text-sm ${allApproved ? "text-success-700" : "text-error-700"}`}>
        {allApproved
          ? "All variants approved — ready to submit for policy review."
          : `${approvedCount} of ${variants.length} variants approved. Every variant must be approved before you can submit for policy review.`}
      </p>
    </div>
  );
}
