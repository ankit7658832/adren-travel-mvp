import { useParams } from "react-router-dom";
import { Button } from "@/shared/design-system/Button";
import { useCampaignBillingDetail } from "./useCampaignBillingDetail";

/**
 * PRD §14.3 — a Consultant's billing-transparency detail view for a
 * single campaign (ADS-11): spend-to-date, budget cap, and a
 * per-transaction breakdown all visible together, not summarized into
 * one opaque figure (this story's own AC). Consolidates ADS-05/06/10's
 * individual guardrails into one coherent screen rather than a new
 * business rule of its own.
 *
 * States implemented: loading (fetching the detail), error (fetch
 * failed), empty (no spend recorded yet — a legitimate sub-state of
 * success, not a separate top-level branch, since budget/cap are still
 * shown), success (the full breakdown). No separate "default" state —
 * same reasoning as CampaignBuilder's own docstring: the fetch starts
 * immediately on mount, so there's nothing meaningfully distinct to show
 * before loading begins.
 */
export function CampaignBillingDetail() {
  const { campaignId } = useParams<{ campaignId: string }>();
  const detailQuery = useCampaignBillingDetail(campaignId ?? "");

  if (detailQuery.isLoading) {
    return (
      <main className="mx-auto max-w-2xl px-6 py-8">
        <h1 className="text-2xl font-semibold text-neutral-900">Campaign Billing</h1>
        <p role="status" className="mt-4 text-sm text-neutral-600">
          Loading billing detail…
        </p>
      </main>
    );
  }

  if (detailQuery.isError) {
    return (
      <main className="mx-auto max-w-2xl px-6 py-8">
        <h1 className="text-2xl font-semibold text-neutral-900">Campaign Billing</h1>
        <div role="alert" className="mt-4 flex items-center justify-between rounded-md border border-error-600/20 bg-error-50 px-4 py-3">
          <p className="text-sm text-error-700">Could not load billing detail.</p>
          <Button variant="secondary" size="sm" onClick={() => detailQuery.refetch()}>
            Retry
          </Button>
        </div>
      </main>
    );
  }

  const detail = detailQuery.data;
  if (!detail) {
    return null;
  }

  return (
    <main className="mx-auto max-w-2xl px-6 py-8">
      <h1 className="text-2xl font-semibold text-neutral-900">Campaign Billing</h1>

      <div className="mt-4 flex items-center justify-between rounded-md border border-neutral-200 bg-surface px-4 py-3">
        <div>
          <p className="text-sm text-neutral-600">Spend to date</p>
          <p className="text-lg font-semibold text-neutral-900">
            {detail.spendToDateAmount} {detail.budgetCapCurrency}
          </p>
        </div>
        <div className="text-right">
          <p className="text-sm text-neutral-600">Budget cap</p>
          <p className="text-lg font-semibold text-neutral-900">
            {detail.budgetCapAmount ?? "—"} {detail.budgetCapCurrency}
          </p>
        </div>
      </div>

      <h2 className="mt-6 text-lg font-medium text-neutral-900">Transactions</h2>
      {detail.transactions.length === 0 ? (
        <p className="mt-2 text-sm text-neutral-600">No spend has been recorded yet.</p>
      ) : (
        <ul aria-label="spend-transactions" className="mt-3 space-y-2">
          {detail.transactions.map((transaction) => (
            <li
              key={transaction.transactionId}
              className="flex items-center justify-between rounded-md border border-neutral-200 bg-surface px-4 py-2"
            >
              <span className="text-sm text-neutral-600">{new Date(transaction.recordedAt).toLocaleString()}</span>
              <span className="text-sm font-medium text-neutral-900">
                {transaction.amount} {detail.budgetCapCurrency}
              </span>
            </li>
          ))}
        </ul>
      )}
    </main>
  );
}
