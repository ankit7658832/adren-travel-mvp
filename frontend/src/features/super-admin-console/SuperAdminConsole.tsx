import { SuperAdminDashboard } from "@/features/super-admin-dashboard/SuperAdminDashboard";

/**
 * PRD §21.6 — Super Admin Console. §21.6's own spec: "Left navigation:
 * Consultants, Suppliers, Ad Accounts, AI Governance Logs, Global
 * Reporting" — Consultants/Suppliers/AI Governance/Campaign policy review
 * are each already their own separately-routed screen (/admin/consultants,
 * /admin/suppliers, /admin/ai-governance, /admin/campaigns/policy-review).
 * This landing route renders Global Reporting (HRD-11's {@code
 * SuperAdminDashboard}) directly, matching how {@code ConsultantDashboard}
 * is the landing page at {@code /dashboard} for Consultants.
 */
export function SuperAdminConsole() {
  return (
    <main className="mx-auto max-w-5xl px-6 py-8">
      <h1 className="text-2xl font-semibold text-neutral-900">21.6 Super Admin Console — Global Reporting</h1>
      <div className="mt-6">
        <SuperAdminDashboard />
      </div>
    </main>
  );
}
