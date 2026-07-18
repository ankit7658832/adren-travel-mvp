import { useState, type FormEvent } from "react";
import { Badge } from "@/shared/design-system/Badge";
import { Button } from "@/shared/design-system/Button";
import { useLocalDmcOnboarding } from "./useLocalDmcOnboarding";

const EMPTY_FORM = { businessName: "", productCategories: "", sampleRatesSummary: "", referencesInfo: "" };

/**
 * PRD §10.3 step 1, §20.14, DMC-01 — submit a new Local DMC for onboarding
 * (always Pending until vetted, DMC-02) and browse the caller's own (or,
 * for Super Admin, every Consultant's) submitted records. All 5 PRD Part
 * 21 states (default/loading/success/empty/error).
 */
export function LocalDmcOnboarding() {
  const { listQuery, submit } = useLocalDmcOnboarding();
  const [form, setForm] = useState(EMPTY_FORM);

  function handleSubmit(event: FormEvent) {
    event.preventDefault();
    submit.mutate(
      {
        businessName: form.businessName,
        productCategories: form.productCategories.split(",").map((c) => c.trim()).filter(Boolean),
        sampleRatesSummary: form.sampleRatesSummary,
        referencesInfo: form.referencesInfo,
      },
      { onSuccess: () => setForm(EMPTY_FORM) }
    );
  }

  return (
    <main className="mx-auto max-w-3xl px-6 py-8">
      <h1 className="text-2xl font-semibold text-neutral-900">Local DMC Onboarding</h1>

      <form onSubmit={handleSubmit} className="mt-6 space-y-3 rounded-md border border-neutral-200 bg-surface p-4">
        <div>
          <label htmlFor="dmc-business-name" className="mb-1 block text-sm font-medium text-neutral-700">
            Business name
          </label>
          <input
            id="dmc-business-name"
            required
            value={form.businessName}
            onChange={(e) => setForm((f) => ({ ...f, businessName: e.target.value }))}
            className="h-10 w-full rounded-md border border-neutral-300 bg-surface px-3 text-base text-neutral-900"
          />
        </div>
        <div>
          <label htmlFor="dmc-product-categories" className="mb-1 block text-sm font-medium text-neutral-700">
            Product categories (comma-separated)
          </label>
          <input
            id="dmc-product-categories"
            required
            placeholder="TRANSFER, ACTIVITY"
            value={form.productCategories}
            onChange={(e) => setForm((f) => ({ ...f, productCategories: e.target.value }))}
            className="h-10 w-full rounded-md border border-neutral-300 bg-surface px-3 text-base text-neutral-900"
          />
        </div>
        <div>
          <label htmlFor="dmc-sample-rates" className="mb-1 block text-sm font-medium text-neutral-700">
            Sample rates
          </label>
          <textarea
            id="dmc-sample-rates"
            value={form.sampleRatesSummary}
            onChange={(e) => setForm((f) => ({ ...f, sampleRatesSummary: e.target.value }))}
            className="w-full rounded-md border border-neutral-300 bg-surface px-3 py-2 text-base text-neutral-900"
            rows={2}
          />
        </div>
        <div>
          <label htmlFor="dmc-references" className="mb-1 block text-sm font-medium text-neutral-700">
            References
          </label>
          <textarea
            id="dmc-references"
            value={form.referencesInfo}
            onChange={(e) => setForm((f) => ({ ...f, referencesInfo: e.target.value }))}
            className="w-full rounded-md border border-neutral-300 bg-surface px-3 py-2 text-base text-neutral-900"
            rows={2}
          />
        </div>
        {submit.isError && (
          <p role="alert" className="text-sm text-error-700">
            Could not submit this Local DMC.
          </p>
        )}
        <Button type="submit" disabled={submit.isPending}>
          Submit for onboarding
        </Button>
      </form>

      <div className="mt-8">
        {listQuery.isLoading && (
          <p role="status" className="text-sm text-neutral-600">
            Loading Local DMCs…
          </p>
        )}

        {listQuery.isError && (
          <div
            role="alert"
            className="flex items-center justify-between rounded-md border border-error-600/20 bg-error-50 px-4 py-3"
          >
            <p className="text-sm text-error-700">Could not load Local DMCs.</p>
            <Button variant="secondary" size="sm" onClick={() => listQuery.refetch()}>
              Retry
            </Button>
          </div>
        )}

        {listQuery.isSuccess && listQuery.data.content.length === 0 && (
          <p className="text-sm text-neutral-600">No Local DMCs submitted yet.</p>
        )}

        {listQuery.isSuccess && listQuery.data.content.length > 0 && (
          <ul aria-label="local-dmc-list" className="space-y-3">
            {listQuery.data.content.map((dmc) => (
              <li
                key={dmc.localDmcId}
                className="flex items-center justify-between rounded-md border border-neutral-200 bg-surface px-4 py-3"
              >
                <div>
                  <p className="text-base text-neutral-900">{dmc.businessName}</p>
                  <p className="text-sm text-neutral-600">{dmc.productCategories.join(", ")}</p>
                </div>
                <div className="flex items-center gap-2">
                  {dmc.flagged && <Badge tone="error">Flagged: quality threshold exceeded</Badge>}
                  <Badge tone={dmc.status === "ACTIVE" ? "success" : "neutral"}>{dmc.status}</Badge>
                </div>
              </li>
            ))}
          </ul>
        )}
      </div>
    </main>
  );
}
