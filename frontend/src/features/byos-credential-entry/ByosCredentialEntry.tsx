import { useState, type FormEvent } from "react";
import { Badge } from "@/shared/design-system/Badge";
import { Button } from "@/shared/design-system/Button";
import { BYOS_SUPPLIER_IDS, useByosCredentialEntry, type ByosSupplierId } from "./useByosCredentialEntry";

/**
 * PRD §10.4, DMC-06 — a Consultant enters their own supplier API
 * credentials (BYOS) so that supplier's inventory is scoped to their
 * account only. The secret value is write-only from this screen: the
 * backend never returns it, only a masked configured/not-configured
 * summary (FND-12's row-level encryption). All 5 PRD Part 21 states.
 */
export function ByosCredentialEntry() {
  const [selectedSupplier, setSelectedSupplier] = useState<ByosSupplierId>("HOTELBEDS");
  const [secretValue, setSecretValue] = useState("");
  const { listQuery, save } = useByosCredentialEntry();

  function handleSubmit(event: FormEvent) {
    event.preventDefault();
    save.mutate({ supplierId: selectedSupplier, secretValue }, { onSuccess: () => setSecretValue("") });
  }

  const summaryFor = (supplierId: ByosSupplierId) => listQuery.data?.find((c) => c.supplierId === supplierId);

  return (
    <main className="mx-auto max-w-3xl px-6 py-8">
      <h1 className="text-2xl font-semibold text-neutral-900">Your Supplier Credentials (BYOS)</h1>
      <p className="mt-1 text-sm text-neutral-600">
        Connect your own supplier accounts — inventory from these credentials is available only to you.
      </p>

      <form onSubmit={handleSubmit} className="mt-6 flex flex-col gap-2 sm:flex-row sm:items-end sm:gap-3">
        <div>
          <label htmlFor="byos-supplier-select" className="mb-1 block text-sm font-medium text-neutral-700">
            Supplier
          </label>
          <select
            id="byos-supplier-select"
            value={selectedSupplier}
            onChange={(e) => setSelectedSupplier(e.target.value as ByosSupplierId)}
            className="h-10 rounded-md border border-neutral-300 bg-surface px-3 text-base text-neutral-900 focus:outline-none focus-visible:ring-2 focus-visible:ring-focus-ring focus-visible:ring-offset-2"
          >
            {BYOS_SUPPLIER_IDS.map((id) => (
              <option key={id} value={id}>
                {id}
              </option>
            ))}
          </select>
        </div>
        <div className="flex-1">
          <label htmlFor="byos-secret-value" className="mb-1 block text-sm font-medium text-neutral-700">
            Your credential value
          </label>
          <input
            id="byos-secret-value"
            type="password"
            required
            value={secretValue}
            onChange={(e) => setSecretValue(e.target.value)}
            className="h-10 w-full rounded-md border border-neutral-300 bg-surface px-3 text-base text-neutral-900 focus:outline-none focus-visible:ring-2 focus-visible:ring-focus-ring focus-visible:ring-offset-2"
          />
        </div>
        <Button type="submit" disabled={save.isPending}>
          {save.isPending ? "Saving…" : "Save"}
        </Button>
      </form>
      {save.isError && (
        <p role="alert" className="mt-2 text-sm text-error-700">
          Could not save this credential. Please try again.
        </p>
      )}
      {save.isSuccess && (
        <p role="status" className="mt-2 text-sm text-success-700">
          Credential saved.
        </p>
      )}

      <div className="mt-8">
        {listQuery.isLoading && (
          <p role="status" className="text-sm text-neutral-600">
            Loading your supplier credentials…
          </p>
        )}

        {listQuery.isError && (
          <div
            role="alert"
            className="flex items-center justify-between rounded-md border border-error-600/20 bg-error-50 px-4 py-3"
          >
            <p className="text-sm text-error-700">Could not load your supplier credentials.</p>
            <Button variant="secondary" size="sm" onClick={() => listQuery.refetch()}>
              Retry
            </Button>
          </div>
        )}

        {listQuery.isSuccess && (
          <ul aria-label="byos-credential-list" className="space-y-3">
            {BYOS_SUPPLIER_IDS.map((id) => {
              const summary = summaryFor(id);
              return (
                <li
                  key={id}
                  className="flex items-center justify-between rounded-md border border-neutral-200 bg-surface px-4 py-3"
                >
                  <span className="text-base text-neutral-900">{id}</span>
                  {summary?.configured ? (
                    <Badge tone="success">Configured — •••• masked</Badge>
                  ) : (
                    <Badge tone="neutral">Not configured</Badge>
                  )}
                </li>
              );
            })}
          </ul>
        )}
      </div>
    </main>
  );
}
