import { useState, type FormEvent } from "react";
import { useParams } from "react-router-dom";
import { Button } from "@/shared/design-system/Button";
import { useLocalDmcInventory, type LocalDmcInventoryItemView } from "./useLocalDmcInventory";

const CATEGORIES = ["HOTEL", "FLIGHT", "TRANSFER", "CRUISE", "ACTIVITY"];
const CURRENCIES = ["INR", "AUD", "GBP", "USD", "AED", "DKK"];

interface EditFormState {
  productName: string;
  category: string;
  netRate: string;
  netRateCurrency: string;
  cancellationPolicyText: string;
  availableFrom: string;
  availableTo: string;
}

function toFormState(item: LocalDmcInventoryItemView): EditFormState {
  return {
    productName: item.productName,
    category: item.category,
    netRate: String(item.netRate),
    netRateCurrency: item.netRateCurrency,
    cancellationPolicyText: item.cancellationPolicyText,
    availableFrom: item.availableFrom,
    availableTo: item.availableTo,
  };
}

/**
 * PRD §10.2.8, DMC-10 — edit a Local DMC's already-uploaded inventory items
 * one at a time (rate corrections, policy text changes) rather than only
 * being able to bulk-replace via CSV. All 5 PRD Part 21 states.
 */
export function LocalDmcInventoryManagement() {
  const { id: localDmcId } = useParams<{ id: string }>();
  const { listQuery, update } = useLocalDmcInventory(localDmcId ?? "");
  const [editingItemId, setEditingItemId] = useState<string | null>(null);
  const [form, setForm] = useState<EditFormState | null>(null);

  function startEditing(item: LocalDmcInventoryItemView) {
    setEditingItemId(item.itemId);
    setForm(toFormState(item));
  }

  function cancelEditing() {
    setEditingItemId(null);
    setForm(null);
  }

  function handleSubmit(event: FormEvent) {
    event.preventDefault();
    if (!editingItemId || !form) {
      return;
    }
    update.mutate(
      {
        itemId: editingItemId,
        productName: form.productName,
        category: form.category,
        netRate: Number(form.netRate),
        netRateCurrency: form.netRateCurrency,
        cancellationPolicyText: form.cancellationPolicyText,
        availableFrom: form.availableFrom,
        availableTo: form.availableTo,
      },
      { onSuccess: () => cancelEditing() }
    );
  }

  return (
    <section>
      <h2 className="text-2xl font-semibold text-neutral-900">Manage inventory items</h2>

      <div className="mt-6">
        {listQuery.isLoading && (
          <p role="status" className="text-sm text-neutral-600">
            Loading inventory…
          </p>
        )}

        {listQuery.isError && (
          <div
            role="alert"
            className="flex items-center justify-between rounded-md border border-error-600/20 bg-error-50 px-4 py-3"
          >
            <p className="text-sm text-error-700">Could not load inventory items.</p>
            <Button variant="secondary" size="sm" onClick={() => listQuery.refetch()}>
              Retry
            </Button>
          </div>
        )}

        {listQuery.isSuccess && listQuery.data.content.length === 0 && (
          <p className="text-sm text-neutral-600">No inventory items uploaded yet.</p>
        )}

        {listQuery.isSuccess && listQuery.data.content.length > 0 && (
          <ul aria-label="local-dmc-inventory-list" className="space-y-3">
            {listQuery.data.content.map((item) =>
              editingItemId === item.itemId && form ? (
                <li key={item.itemId} className="rounded-md border border-neutral-200 bg-surface p-4">
                  <form onSubmit={handleSubmit} className="space-y-3">
                    <div>
                      <label htmlFor={`item-name-${item.itemId}`} className="mb-1 block text-sm font-medium text-neutral-700">
                        Product name
                      </label>
                      <input
                        id={`item-name-${item.itemId}`}
                        required
                        value={form.productName}
                        onChange={(e) => setForm((f) => (f ? { ...f, productName: e.target.value } : f))}
                        className="h-10 w-full rounded-md border border-neutral-300 bg-surface px-3 text-base text-neutral-900"
                      />
                    </div>
                    <div>
                      <label htmlFor={`item-category-${item.itemId}`} className="mb-1 block text-sm font-medium text-neutral-700">
                        Category
                      </label>
                      <select
                        id={`item-category-${item.itemId}`}
                        value={form.category}
                        onChange={(e) => setForm((f) => (f ? { ...f, category: e.target.value } : f))}
                        className="h-10 w-full rounded-md border border-neutral-300 bg-surface px-3 text-base text-neutral-900"
                      >
                        {CATEGORIES.map((c) => (
                          <option key={c} value={c}>
                            {c}
                          </option>
                        ))}
                      </select>
                    </div>
                    <div className="flex gap-3">
                      <div className="flex-1">
                        <label htmlFor={`item-rate-${item.itemId}`} className="mb-1 block text-sm font-medium text-neutral-700">
                          Net rate
                        </label>
                        <input
                          id={`item-rate-${item.itemId}`}
                          type="number"
                          required
                          value={form.netRate}
                          onChange={(e) => setForm((f) => (f ? { ...f, netRate: e.target.value } : f))}
                          className="h-10 w-full rounded-md border border-neutral-300 bg-surface px-3 text-base text-neutral-900"
                        />
                      </div>
                      <div className="w-32">
                        <label htmlFor={`item-currency-${item.itemId}`} className="mb-1 block text-sm font-medium text-neutral-700">
                          Currency
                        </label>
                        <select
                          id={`item-currency-${item.itemId}`}
                          value={form.netRateCurrency}
                          onChange={(e) => setForm((f) => (f ? { ...f, netRateCurrency: e.target.value } : f))}
                          className="h-10 w-full rounded-md border border-neutral-300 bg-surface px-3 text-base text-neutral-900"
                        >
                          {CURRENCIES.map((c) => (
                            <option key={c} value={c}>
                              {c}
                            </option>
                          ))}
                        </select>
                      </div>
                    </div>
                    <div>
                      <label htmlFor={`item-policy-${item.itemId}`} className="mb-1 block text-sm font-medium text-neutral-700">
                        Cancellation policy
                      </label>
                      <textarea
                        id={`item-policy-${item.itemId}`}
                        required
                        value={form.cancellationPolicyText}
                        onChange={(e) => setForm((f) => (f ? { ...f, cancellationPolicyText: e.target.value } : f))}
                        className="w-full rounded-md border border-neutral-300 bg-surface px-3 py-2 text-base text-neutral-900"
                        rows={2}
                      />
                    </div>
                    <div className="flex gap-3">
                      <div className="flex-1">
                        <label htmlFor={`item-from-${item.itemId}`} className="mb-1 block text-sm font-medium text-neutral-700">
                          Available from
                        </label>
                        <input
                          id={`item-from-${item.itemId}`}
                          type="date"
                          required
                          value={form.availableFrom}
                          onChange={(e) => setForm((f) => (f ? { ...f, availableFrom: e.target.value } : f))}
                          className="h-10 w-full rounded-md border border-neutral-300 bg-surface px-3 text-base text-neutral-900"
                        />
                      </div>
                      <div className="flex-1">
                        <label htmlFor={`item-to-${item.itemId}`} className="mb-1 block text-sm font-medium text-neutral-700">
                          Available to
                        </label>
                        <input
                          id={`item-to-${item.itemId}`}
                          type="date"
                          required
                          value={form.availableTo}
                          onChange={(e) => setForm((f) => (f ? { ...f, availableTo: e.target.value } : f))}
                          className="h-10 w-full rounded-md border border-neutral-300 bg-surface px-3 text-base text-neutral-900"
                        />
                      </div>
                    </div>
                    {update.isError && (
                      <p role="alert" className="text-sm text-error-700">
                        Could not save this inventory item.
                      </p>
                    )}
                    <div className="flex gap-3">
                      <Button type="submit" disabled={update.isPending}>
                        Save
                      </Button>
                      <Button type="button" variant="secondary" onClick={cancelEditing}>
                        Cancel
                      </Button>
                    </div>
                  </form>
                </li>
              ) : (
                <li
                  key={item.itemId}
                  className="flex items-center justify-between rounded-md border border-neutral-200 bg-surface px-4 py-3"
                >
                  <div>
                    <p className="text-base text-neutral-900">{item.productName}</p>
                    <p className="text-sm text-neutral-600">
                      {item.category} · {item.netRate} {item.netRateCurrency}
                    </p>
                  </div>
                  <Button variant="secondary" size="sm" onClick={() => startEditing(item)}>
                    Edit
                  </Button>
                </li>
              )
            )}
          </ul>
        )}
      </div>
    </section>
  );
}
