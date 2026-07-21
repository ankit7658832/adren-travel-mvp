import { useState } from "react";
import { Button } from "@/shared/design-system/Button";
import { Card } from "@/shared/design-system/Card";
import { useConsultantDashboard } from "./useConsultantDashboard";
import { ActiveCampaignsTab } from "./ActiveCampaignsTab";

type DashboardTab = "topPackages" | "pendingQuotations" | "activeCampaigns";

/**
 * PRD §9.5/§21.5 — the Consultant Dashboard (HRD-09): summary cards
 * (bookings this month, GMV, wallet balance) and tabs (Top Packages,
 * Pending Quotations, Active Campaigns), all populated from the real
 * composite {@code GET /dashboard/consultant} endpoint. The Active
 * Campaigns tab renders ADS-09's already-built {@code ActiveCampaignsTab}
 * directly (its own self-fetching component) rather than the composite
 * response's embedded {@code activeCampaigns} field, per that story's own
 * "HRD-09 composes this component" design intent — a small duplicate
 * fetch, accepted in exchange for not duplicating campaign-row rendering
 * in two places.
 *
 * States implemented: loading, error (retry), empty (HRD-10's onboarding
 * checklist — a new Consultant with zero bookings this month, in place of
 * zeroed-out charts), success (the full dashboard). No separate "default"
 * state, same reasoning as every other dashboard-shaped screen this
 * session: the fetch starts immediately on mount.
 */
export function ConsultantDashboard() {
  const dashboardQuery = useConsultantDashboard();
  const [activeTab, setActiveTab] = useState<DashboardTab>("topPackages");

  if (dashboardQuery.isLoading) {
    return (
      <main className="mx-auto max-w-4xl px-6 py-8">
        <h1 className="text-2xl font-semibold text-neutral-900">21.5 Consultant Dashboard</h1>
        <p role="status" className="mt-4 text-sm text-neutral-600">
          Loading your dashboard…
        </p>
      </main>
    );
  }

  if (dashboardQuery.isError) {
    return (
      <main className="mx-auto max-w-4xl px-6 py-8">
        <h1 className="text-2xl font-semibold text-neutral-900">21.5 Consultant Dashboard</h1>
        <div role="alert" className="mt-4 flex items-center justify-between rounded-md border border-error-600/20 bg-error-50 px-4 py-3">
          <p className="text-sm text-error-700">Could not load your dashboard.</p>
          <Button variant="secondary" size="sm" onClick={() => dashboardQuery.refetch()}>
            Retry
          </Button>
        </div>
      </main>
    );
  }

  const dashboard = dashboardQuery.data;
  if (!dashboard) {
    return null;
  }

  // HRD-10, PRD §21.5's empty-state requirement: a new Consultant with no
  // activity yet sees a getting-started checklist, not a dashboard full of
  // zeroed-out charts. "Zero bookings" is approximated as zero this month
  // — the field HRD-09's own metrics carry — since a genuinely new
  // Consultant has zero activity across the board, not just this month.
  if (dashboard.metrics.bookingsThisMonth === 0) {
    return (
      <main className="mx-auto max-w-4xl px-6 py-8">
        <h1 className="text-2xl font-semibold text-neutral-900">21.5 Consultant Dashboard</h1>
        <Card className="mt-6">
          <h2 className="text-lg font-medium text-neutral-900">Welcome — let&apos;s get you started</h2>
          <ul aria-label="onboarding-checklist" className="mt-4 space-y-3">
            <li className="flex items-center gap-2 text-sm text-neutral-700">
              <span aria-hidden="true">1.</span> Search for inventory and build your first itinerary.
            </li>
            <li className="flex items-center gap-2 text-sm text-neutral-700">
              <span aria-hidden="true">2.</span> Save it as a Quotation and share it with a traveler.
            </li>
            <li className="flex items-center gap-2 text-sm text-neutral-700">
              <span aria-hidden="true">3.</span> Convert it to a Package to make it reusable and bookable.
            </li>
            <li className="flex items-center gap-2 text-sm text-neutral-700">
              <span aria-hidden="true">4.</span> Confirm your first booking — it will show up here.
            </li>
          </ul>
        </Card>
      </main>
    );
  }

  return (
    <main className="mx-auto max-w-4xl px-6 py-8">
      <h1 className="text-2xl font-semibold text-neutral-900">21.5 Consultant Dashboard</h1>

      <div className="mt-6 grid grid-cols-3 gap-4">
        <Card padding="sm">
          <p className="text-sm text-neutral-600">Bookings this month</p>
          <p className="mt-1 text-2xl font-semibold text-neutral-900">{dashboard.metrics.bookingsThisMonth}</p>
        </Card>
        <Card padding="sm">
          <p className="text-sm text-neutral-600">GMV this month</p>
          <p className="mt-1 text-2xl font-semibold text-neutral-900">
            {dashboard.metrics.gmvThisMonth.amount} {dashboard.metrics.gmvThisMonth.currency}
          </p>
        </Card>
        <Card padding="sm">
          <p className="text-sm text-neutral-600">Wallet balance</p>
          <p className="mt-1 text-2xl font-semibold text-neutral-900">
            {dashboard.wallet.availableBalance} {dashboard.wallet.currency}
          </p>
        </Card>
      </div>

      <div role="tablist" aria-label="dashboard-tabs" className="mt-8 flex gap-2 border-b border-neutral-200">
        <TabButton label="Top Packages" isActive={activeTab === "topPackages"} onClick={() => setActiveTab("topPackages")} />
        <TabButton
          label="Pending Quotations"
          isActive={activeTab === "pendingQuotations"}
          onClick={() => setActiveTab("pendingQuotations")}
        />
        <TabButton
          label="Active Campaigns"
          isActive={activeTab === "activeCampaigns"}
          onClick={() => setActiveTab("activeCampaigns")}
        />
      </div>

      <div className="mt-4">
        {activeTab === "topPackages" && (
          dashboard.topPackages.length === 0 ? (
            <p className="text-sm text-neutral-600">You have no published packages yet.</p>
          ) : (
            <ul aria-label="top-packages" className="space-y-2">
              {dashboard.topPackages.map((pkg) => (
                <li key={pkg.packageId} className="flex items-center justify-between rounded-md border border-neutral-200 bg-surface px-4 py-2">
                  <span className="text-sm text-neutral-900">{pkg.name}</span>
                  <span className="text-sm text-neutral-600">{pkg.bookingCount} bookings</span>
                </li>
              ))}
            </ul>
          )
        )}
        {activeTab === "pendingQuotations" && (
          dashboard.pendingQuotations.length === 0 ? (
            <p className="text-sm text-neutral-600">You have no pending quotations.</p>
          ) : (
            <ul aria-label="pending-quotations" className="space-y-2">
              {dashboard.pendingQuotations.map((quotation) => (
                <li key={quotation.itineraryId} className="rounded-md border border-neutral-200 bg-surface px-4 py-2">
                  <span className="text-sm text-neutral-600">{new Date(quotation.createdAt).toLocaleDateString()}</span>
                </li>
              ))}
            </ul>
          )
        )}
        {activeTab === "activeCampaigns" && <ActiveCampaignsTab />}
      </div>
    </main>
  );
}

function TabButton({ label, isActive, onClick }: { label: string; isActive: boolean; onClick: () => void }) {
  return (
    <button
      type="button"
      role="tab"
      aria-selected={isActive}
      onClick={onClick}
      className={
        isActive
          ? "border-b-2 border-primary-600 px-3 py-2 text-sm font-medium text-primary-600"
          : "border-b-2 border-transparent px-3 py-2 text-sm font-medium text-neutral-600"
      }
    >
      {label}
    </button>
  );
}
