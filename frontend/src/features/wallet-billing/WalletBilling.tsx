import { Button } from "@/shared/design-system/Button";
import { Badge } from "@/shared/design-system/Badge";
import { LEDGER_ENTRY_TYPES, useWalletBilling, type LedgerEntryType } from "./useWalletBilling";

/**
 * PRD §21.7 — Wallet & Billing screen (FIN-09): balance/credit-limit/
 * pending-holds summary plus a transaction ledger filterable by type. All
 * 5 PRD Part 21 states (default/loading/success/empty/error) per
 * RULES.md §7.2, independently for the summary and the ledger since they
 * are two separate queries that can fail/load independently.
 * <p>
 * PRD §21.7's pre-payment credit-limit breach warning (this story's other
 * AC — "before they reach payment, not after") is {@link
 * CreditLimitBreachWarning}, a separate reusable component composed into
 * whichever pre-payment screen has a known pending amount (Package
 * Builder's publish step today) — not this screen, which only ever shows
 * the Consultant's already-settled ledger.
 */
export function WalletBilling() {
  const { walletQuery, ledgerQuery, typeFilter, setTypeFilter } = useWalletBilling();

  return (
    <main className="mx-auto max-w-3xl px-6 py-8">
      <h1 className="text-2xl font-semibold text-neutral-900">21.7 Wallet &amp; Billing</h1>

      <section className="mt-6">
        {walletQuery.isLoading && (
          <p role="status" className="text-sm text-neutral-600">
            Loading wallet…
          </p>
        )}
        {walletQuery.isError && (
          <div role="alert" className="flex items-center justify-between rounded-md border border-error-600/20 bg-error-50 px-4 py-3">
            <p className="text-sm text-error-700">Could not load your wallet.</p>
            <Button variant="secondary" size="sm" onClick={() => walletQuery.refetch()}>
              Retry
            </Button>
          </div>
        )}
        {walletQuery.isSuccess && (
          <dl className="grid grid-cols-3 gap-4 rounded-md border border-neutral-200 bg-surface px-4 py-3">
            <div>
              <dt className="text-sm text-neutral-600">Available balance</dt>
              <dd className="mt-1 text-lg font-medium text-neutral-900">
                {walletQuery.data.currency} {walletQuery.data.availableBalance}
              </dd>
            </div>
            <div>
              <dt className="text-sm text-neutral-600">Credit limit</dt>
              <dd className="mt-1 text-lg font-medium text-neutral-900">
                {walletQuery.data.currency} {walletQuery.data.creditLimit}
              </dd>
            </div>
            <div>
              <dt className="text-sm text-neutral-600">Pending holds</dt>
              <dd className="mt-1 text-lg font-medium text-neutral-900">
                {walletQuery.data.currency} {walletQuery.data.pendingHolds}
              </dd>
            </div>
          </dl>
        )}
      </section>

      <section className="mt-8">
        <div className="flex items-center justify-between">
          <h2 className="text-lg font-medium text-neutral-900">Transaction ledger</h2>
          <label className="flex items-center gap-2 text-sm text-neutral-700">
            Filter by type
            <select
              aria-label="filter-ledger-by-type"
              value={typeFilter ?? ""}
              onChange={(e) => setTypeFilter((e.target.value || null) as LedgerEntryType | null)}
              className="h-9 rounded-md border border-neutral-300 bg-surface px-2 text-sm text-neutral-900"
            >
              <option value="">All types</option>
              {LEDGER_ENTRY_TYPES.map((type) => (
                <option key={type} value={type}>
                  {type}
                </option>
              ))}
            </select>
          </label>
        </div>

        <div className="mt-4">
          {ledgerQuery.isLoading && (
            <p role="status" className="text-sm text-neutral-600">
              Loading transactions…
            </p>
          )}
          {ledgerQuery.isError && (
            <div role="alert" className="flex items-center justify-between rounded-md border border-error-600/20 bg-error-50 px-4 py-3">
              <p className="text-sm text-error-700">Could not load your transaction ledger.</p>
              <Button variant="secondary" size="sm" onClick={() => ledgerQuery.refetch()}>
                Retry
              </Button>
            </div>
          )}
          {ledgerQuery.isSuccess && ledgerQuery.data.content.length === 0 && (
            <p className="text-sm text-neutral-600">
              {typeFilter ? `No ${typeFilter} transactions yet.` : "No transactions yet."}
            </p>
          )}
          {ledgerQuery.isSuccess && ledgerQuery.data.content.length > 0 && (
            <ul aria-label="wallet-ledger" className="space-y-2">
              {ledgerQuery.data.content.map((entry) => (
                <li
                  key={entry.ledgerEntryId}
                  className="flex items-center justify-between rounded-md border border-neutral-200 bg-surface px-4 py-3"
                >
                  <div className="flex items-center gap-3">
                    <Badge tone="neutral">{entry.type}</Badge>
                    <span className="text-sm text-neutral-600">
                      {new Date(entry.createdAt).toLocaleString()}
                    </span>
                  </div>
                  <span className="text-base font-medium text-neutral-900">
                    {entry.currency} {entry.amount}
                  </span>
                </li>
              ))}
            </ul>
          )}
        </div>
      </section>
    </main>
  );
}
