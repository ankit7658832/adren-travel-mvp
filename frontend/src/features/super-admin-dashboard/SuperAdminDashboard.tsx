import { Button } from "@/shared/design-system/Button";
import { Card } from "@/shared/design-system/Card";
import { useSuperAdminDashboard } from "./useSuperAdminDashboard";

/**
 * PRD §9.5/§21.6 — the Super Admin Dashboard / Global Reporting (HRD-11):
 * all-Consultant GMV, per-supplier performance, an AI governance summary,
 * and ad spend across Consultants, platform scope, all populated from the
 * real composite {@code GET /dashboard/super-admin} endpoint.
 *
 * States implemented: loading, error (retry), empty (no data recorded
 * anywhere yet — every currency-grouped breakdown is empty and the AI
 * governance summary is zero), success (the full report). No separate
 * "default" state, same reasoning as every other dashboard screen this
 * session.
 */
export function SuperAdminDashboard() {
  const dashboardQuery = useSuperAdminDashboard();

  if (dashboardQuery.isLoading) {
    return (
      <section>
        <p role="status" className="text-sm text-neutral-600">
          Loading global reporting…
        </p>
      </section>
    );
  }

  if (dashboardQuery.isError) {
    return (
      <section>
        <div role="alert" className="flex items-center justify-between rounded-md border border-error-600/20 bg-error-50 px-4 py-3">
          <p className="text-sm text-error-700">Could not load global reporting.</p>
          <Button variant="secondary" size="sm" onClick={() => dashboardQuery.refetch()}>
            Retry
          </Button>
        </div>
      </section>
    );
  }

  const dashboard = dashboardQuery.data;
  if (!dashboard) {
    return null;
  }

  const hasAnyGmv = dashboard.gmv.gmvByCurrency.length > 0;
  const hasAnyAdSpend = dashboard.adSpend.spendByCurrency.length > 0;
  const hasAnySupplierActivity = dashboard.supplierPerformance.some((s) => s.lineItemCount > 0);

  if (!hasAnyGmv && !hasAnyAdSpend && !hasAnySupplierActivity && dashboard.aiGovernanceSummary.totalSuggestions === 0) {
    return (
      <section>
        <p className="text-sm text-neutral-600">No platform activity has been recorded yet.</p>
      </section>
    );
  }

  return (
    <section className="space-y-6">
      <Card padding="sm">
        <h2 className="text-sm font-medium text-neutral-600">All-Consultant GMV</h2>
        {hasAnyGmv ? (
          <ul aria-label="gmv-by-currency" className="mt-2 space-y-1">
            {dashboard.gmv.gmvByCurrency.map((entry) => (
              <li key={entry.currency} className="text-lg font-semibold text-neutral-900">
                {entry.amount} {entry.currency}
              </li>
            ))}
          </ul>
        ) : (
          <p className="mt-2 text-sm text-neutral-600">No bookings recorded yet.</p>
        )}
      </Card>

      <Card padding="sm">
        <h2 className="text-sm font-medium text-neutral-600">Supplier Performance</h2>
        <ul aria-label="supplier-performance" className="mt-2 grid grid-cols-3 gap-2">
          {dashboard.supplierPerformance.map((supplier) => (
            <li key={supplier.supplierId} className="rounded-md border border-neutral-200 px-3 py-2">
              <p className="text-xs text-neutral-600">{supplier.supplierId}</p>
              <p className="text-sm font-medium text-neutral-900">{supplier.lineItemCount}</p>
            </li>
          ))}
        </ul>
      </Card>

      <Card padding="sm">
        <h2 className="text-sm font-medium text-neutral-600">AI Governance Summary</h2>
        <p className="mt-2 text-sm text-neutral-700">
          {dashboard.aiGovernanceSummary.totalSuggestions} total suggestions —{" "}
          {dashboard.aiGovernanceSummary.suggestedCount} suggested,{" "}
          {dashboard.aiGovernanceSummary.noViableSuggestionCount} no-viable-suggestion,{" "}
          {dashboard.aiGovernanceSummary.groqErrorCount} errors
        </p>
      </Card>

      <Card padding="sm">
        <h2 className="text-sm font-medium text-neutral-600">Ad Spend Across Consultants</h2>
        {hasAnyAdSpend ? (
          <ul aria-label="ad-spend-by-currency" className="mt-2 space-y-1">
            {dashboard.adSpend.spendByCurrency.map((entry) => (
              <li key={entry.currency} className="text-lg font-semibold text-neutral-900">
                {entry.amount} {entry.currency}
              </li>
            ))}
          </ul>
        ) : (
          <p className="mt-2 text-sm text-neutral-600">No ad spend recorded yet.</p>
        )}
      </Card>
    </section>
  );
}
