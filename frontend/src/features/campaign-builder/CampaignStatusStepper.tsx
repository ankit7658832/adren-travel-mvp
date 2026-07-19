import { Badge } from "@/shared/design-system/Badge";
import { cn } from "@/shared/design-system/cn";

export interface CampaignStatusStepperProps {
  status: string;
}

/**
 * PRD §21.8, §20.13 — a visual progress tracker matching the backend's
 * AdCampaignStatus enum exactly: Pending Approval → Pending Policy Review
 * → Live / Rejected, per PRD §21.8's own stated sequence (Live and
 * Rejected are alternate terminal branches off policy review, not two
 * sequential stages). Paused and SpendCapReached are sub-states of Live
 * (a live campaign that stopped spending, not a new pipeline stage) —
 * shown as a badge alongside the Live step, not a separate node.
 */
const LEADING_STEPS = [
  { key: "PENDING_APPROVAL", label: "Pending Approval" },
  { key: "PENDING_POLICY_REVIEW", label: "Pending Policy Review" },
] as const;

const LIVE_SUB_STATE_LABEL: Record<string, string> = {
  PAUSED: "Paused",
  SPEND_CAP_REACHED: "Spend Cap Reached",
};

export function CampaignStatusStepper({ status }: CampaignStatusStepperProps) {
  const isRejected = status === "REJECTED";
  const liveSubStateLabel = LIVE_SUB_STATE_LABEL[status];
  const activeLeadingIndex = LEADING_STEPS.findIndex((step) => step.key === status);
  // Any of the "went live" statuses (LIVE itself, or a sub-state of it)
  // means both leading steps are behind the campaign.
  const isAtOrPastLive = status === "LIVE" || Boolean(liveSubStateLabel) || isRejected;

  return (
    <ol aria-label="campaign-status-stepper" className="flex items-center gap-2">
      {LEADING_STEPS.map((step, index) => {
        const isCurrent = index === activeLeadingIndex;
        const isCompleted = isAtOrPastLive || index < activeLeadingIndex;

        return (
          <li key={step.key} className="flex items-center gap-2">
            <span
              className={cn(
                "rounded-full px-3 py-1 text-xs font-medium",
                isCurrent
                  ? "bg-primary-600 text-white"
                  : isCompleted
                    ? "bg-success-50 text-success-700"
                    : "bg-neutral-100 text-neutral-700"
              )}
              aria-current={isCurrent ? "step" : undefined}
            >
              {step.label}
            </span>
            <span aria-hidden="true" className="text-neutral-400">
              →
            </span>
          </li>
        );
      })}
      <li>
        {isRejected ? (
          <Badge tone="error">Rejected</Badge>
        ) : (
          <div className="flex items-center gap-2">
            <span
              aria-current={status === "LIVE" || Boolean(liveSubStateLabel) ? "step" : undefined}
              className={cn(
                "rounded-full px-3 py-1 text-xs font-medium",
                status === "LIVE" || liveSubStateLabel ? "bg-primary-600 text-white" : "bg-neutral-100 text-neutral-700"
              )}
            >
              Live
            </span>
            {liveSubStateLabel && <Badge tone="warning">{liveSubStateLabel}</Badge>}
          </div>
        )}
      </li>
    </ol>
  );
}
