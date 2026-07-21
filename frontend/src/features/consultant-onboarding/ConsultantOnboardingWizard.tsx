import { useState, type FormEvent } from "react";
import { Button } from "@/shared/design-system/Button";
import { SchemaFieldSet } from "@/shared/forms/SchemaFieldSet";
import { MARKETS, useKycRules, useOnboardConsultant, type Market } from "./useConsultantOnboarding";

/**
 * PRD §13.1/§21.6 — Super Admin's Consultant onboarding wizard. Required
 * fields change per selected market, sourced from FND-04's backend rule
 * table (never a hardcoded per-market conditional here, per RULES.md §24.7).
 */
export function ConsultantOnboardingWizard() {
  const [businessName, setBusinessName] = useState("");
  const [market, setMarket] = useState<Market | null>(null);
  const [kycFieldValues, setKycFieldValues] = useState<Record<string, string>>({});
  const [email, setEmail] = useState("");
  const [initialPassword, setInitialPassword] = useState("");

  const kycRulesQuery = useKycRules(market);
  const onboardMutation = useOnboardConsultant();

  function handleMarketChange(value: string) {
    setMarket(value as Market);
    setKycFieldValues({});
  }

  function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (!market) {
      return;
    }
    onboardMutation.mutate({ businessName, homeMarket: market, kycFields: kycFieldValues, email, initialPassword });
  }

  if (onboardMutation.isSuccess) {
    return (
      <main className="mx-auto max-w-2xl px-6 py-8">
        <div role="status" className="rounded-md border border-success-700/20 bg-success-50 px-4 py-3 text-success-700">
          Consultant onboarded — id {onboardMutation.data.consultantId}
        </div>
      </main>
    );
  }

  return (
    <main className="mx-auto max-w-2xl px-6 py-8">
      <h1 className="text-2xl font-semibold text-neutral-900">Onboard a Consultant</h1>

      <form onSubmit={handleSubmit} className="mt-6 space-y-4">
        <div>
          <label htmlFor="business-name" className="mb-1 block text-sm font-medium text-neutral-700">
            Business name
          </label>
          <input
            id="business-name"
            required
            value={businessName}
            onChange={(e) => setBusinessName(e.target.value)}
            className="h-10 w-full rounded-md border border-neutral-300 bg-surface px-3 text-base text-neutral-900 focus:outline-none focus-visible:ring-2 focus-visible:ring-focus-ring focus-visible:ring-offset-2"
          />
        </div>

        <div>
          <label htmlFor="home-market" className="mb-1 block text-sm font-medium text-neutral-700">
            Home market
          </label>
          <select
            id="home-market"
            required
            value={market ?? ""}
            onChange={(e) => handleMarketChange(e.target.value)}
            className="h-10 w-full rounded-md border border-neutral-300 bg-surface px-3 text-base text-neutral-900 focus:outline-none focus-visible:ring-2 focus-visible:ring-focus-ring focus-visible:ring-offset-2"
          >
            <option value="" disabled>
              Select a market…
            </option>
            {MARKETS.map((m) => (
              <option key={m.value} value={m.value}>
                {m.label}
              </option>
            ))}
          </select>
        </div>

        <div>
          <label htmlFor="consultant-email" className="mb-1 block text-sm font-medium text-neutral-700">
            Login email
          </label>
          <input
            id="consultant-email"
            type="email"
            required
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            className="h-10 w-full rounded-md border border-neutral-300 bg-surface px-3 text-base text-neutral-900 focus:outline-none focus-visible:ring-2 focus-visible:ring-focus-ring focus-visible:ring-offset-2"
          />
        </div>

        <div>
          <label htmlFor="consultant-initial-password" className="mb-1 block text-sm font-medium text-neutral-700">
            Initial password
          </label>
          <input
            id="consultant-initial-password"
            type="password"
            required
            minLength={8}
            value={initialPassword}
            onChange={(e) => setInitialPassword(e.target.value)}
            className="h-10 w-full rounded-md border border-neutral-300 bg-surface px-3 text-base text-neutral-900 focus:outline-none focus-visible:ring-2 focus-visible:ring-focus-ring focus-visible:ring-offset-2"
          />
        </div>

        {market && kycRulesQuery.isLoading && (
          <p role="status" className="text-sm text-neutral-600">
            Loading required fields for {market}…
          </p>
        )}

        {market && kycRulesQuery.isError && (
          <p role="alert" className="text-sm text-error-700">
            Could not load required fields for this market. Please try again.
          </p>
        )}

        {market && kycRulesQuery.isSuccess && kycRulesQuery.data.length === 0 && (
          <p className="text-sm text-neutral-600">No KYC fields are configured for this market yet.</p>
        )}

        {market && kycRulesQuery.isSuccess && kycRulesQuery.data.length > 0 && (
          <SchemaFieldSet
            legend={`KYC details for ${MARKETS.find((m) => m.value === market)?.label}`}
            fields={kycRulesQuery.data}
            values={kycFieldValues}
            onChange={(fieldKey, value) => setKycFieldValues((prev) => ({ ...prev, [fieldKey]: value }))}
          />
        )}

        {onboardMutation.isError && (
          <p role="alert" className="text-sm text-error-700">
            {onboardMutation.error instanceof Error ? onboardMutation.error.message : "Onboarding failed."}
          </p>
        )}

        <Button type="submit" disabled={!market || onboardMutation.isPending}>
          {onboardMutation.isPending ? "Onboarding…" : "Onboard Consultant"}
        </Button>
      </form>
    </main>
  );
}
