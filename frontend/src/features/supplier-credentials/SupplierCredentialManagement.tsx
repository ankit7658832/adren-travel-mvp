import { useState, type FormEvent } from "react";
import { Button } from "@/shared/design-system/Button";
import { Badge } from "@/shared/design-system/Badge";
import {
  SUPPLIER_IDS,
  useSupplierCredentials,
  useUpdateSupplierCredential,
  type SupplierId,
} from "./useSupplierCredentials";

/**
 * PRD §21.6 — Super Admin manages Adren-owned supplier API credentials
 * (FND-10). The secret value is write-only from this screen's point of
 * view: the backend never returns it, only a masked summary.
 */
export function SupplierCredentialManagement() {
  const [selectedSupplier, setSelectedSupplier] = useState<SupplierId>("HOTELBEDS");
  const [secretValue, setSecretValue] = useState("");
  const credentialsQuery = useSupplierCredentials();
  const updateMutation = useUpdateSupplierCredential();

  function handleSubmit(e: FormEvent) {
    e.preventDefault();
    updateMutation.mutate({ supplierId: selectedSupplier, secretValue }, { onSuccess: () => setSecretValue("") });
  }

  const summaryFor = (supplierId: SupplierId) =>
    credentialsQuery.data?.find((c) => c.supplierId === supplierId);

  return (
    <main className="mx-auto max-w-3xl px-6 py-8">
      <h1 className="text-2xl font-semibold text-neutral-900">Supplier Credentials</h1>

      <form onSubmit={handleSubmit} className="mt-6 flex flex-col gap-2 sm:flex-row sm:items-end sm:gap-3">
        <div>
          <label htmlFor="supplier-select" className="mb-1 block text-sm font-medium text-neutral-700">
            Supplier
          </label>
          <select
            id="supplier-select"
            value={selectedSupplier}
            onChange={(e) => setSelectedSupplier(e.target.value as SupplierId)}
            className="h-10 rounded-md border border-neutral-300 bg-surface px-3 text-base text-neutral-900 focus:outline-none focus-visible:ring-2 focus-visible:ring-focus-ring focus-visible:ring-offset-2"
          >
            {SUPPLIER_IDS.map((id) => (
              <option key={id} value={id}>
                {id}
              </option>
            ))}
          </select>
        </div>
        <div className="flex-1">
          <label htmlFor="secret-value" className="mb-1 block text-sm font-medium text-neutral-700">
            New credential value
          </label>
          <input
            id="secret-value"
            type="password"
            required
            value={secretValue}
            onChange={(e) => setSecretValue(e.target.value)}
            className="h-10 w-full rounded-md border border-neutral-300 bg-surface px-3 text-base text-neutral-900 focus:outline-none focus-visible:ring-2 focus-visible:ring-focus-ring focus-visible:ring-offset-2"
          />
        </div>
        <Button type="submit" disabled={updateMutation.isPending}>
          {updateMutation.isPending ? "Saving…" : "Save"}
        </Button>
      </form>
      {updateMutation.isError && (
        <p role="alert" className="mt-2 text-sm text-error-700">
          Could not save the credential. Please try again.
        </p>
      )}
      {updateMutation.isSuccess && (
        <p role="status" className="mt-2 text-sm text-success-700">
          Credential updated.
        </p>
      )}

      <div className="mt-8">
        {credentialsQuery.isLoading && (
          <p role="status" className="text-sm text-neutral-600">
            Loading supplier credentials…
          </p>
        )}

        {credentialsQuery.isError && (
          <div role="alert" className="flex items-center justify-between rounded-md border border-error-600/20 bg-error-50 px-4 py-3">
            <p className="text-sm text-error-700">Could not load supplier credentials.</p>
            <Button variant="secondary" size="sm" onClick={() => credentialsQuery.refetch()}>
              Retry
            </Button>
          </div>
        )}

        {credentialsQuery.isSuccess && (
          <ul aria-label="supplier-credential-list" className="space-y-3">
            {SUPPLIER_IDS.map((id) => {
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
