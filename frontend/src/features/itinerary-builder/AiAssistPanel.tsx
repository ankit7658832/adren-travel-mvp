import { useState, type FormEvent } from "react";
import { Badge } from "@/shared/design-system/Badge";
import { Button } from "@/shared/design-system/Button";
import { useItineraryDraftStore } from "./itineraryDraftStore";
import { useAiAssist, type AiSuggestedLineItemDto, type CompleteWithAiInput } from "./useAiAssist";

function isoDate(daysFromNow: number): string {
  const date = new Date();
  date.setDate(date.getDate() + daysFromNow);
  return date.toISOString().slice(0, 10);
}

const DEFAULT_FORM: CompleteWithAiInput = {
  locationCode: "",
  checkIn: isoDate(30),
  checkOut: isoDate(34),
  naturalLanguageRequest: "",
};

interface AiAssistPanelProps {
  itineraryId: string;
}

/**
 * PRD §21.2's persistent AI-assist entry point / §11.2 principle 2, AI-10
 * — every AI-suggested line item is shown with its source-supplier and
 * availability badges (AI-04's fields) and requires an explicit Accept or
 * Reject; nothing from an AI suggestion is ever added to {@link
 * useItineraryDraftStore} until the Consultant clicks Accept. Renders all
 * 5 PRD Part 21 states: default (collapsed button), the request form,
 * loading, success (SUGGESTION or NO_VIABLE_SUGGESTION), and error.
 */
export function AiAssistPanel({ itineraryId }: AiAssistPanelProps) {
  const [open, setOpen] = useState(false);
  const [form, setForm] = useState<CompleteWithAiInput>(DEFAULT_FORM);
  const { generate, approve } = useAiAssist(itineraryId);
  const setLineItem = useItineraryDraftStore((s) => s.setLineItem);

  function closePanel() {
    setOpen(false);
    setForm(DEFAULT_FORM);
    generate.reset();
    approve.reset();
  }

  function handleSubmit(event: FormEvent) {
    event.preventDefault();
    generate.mutate(form);
  }

  function handleAccept(auditLogId: string, lineItems: AiSuggestedLineItemDto[]) {
    approve.mutate(
      { auditLogId, finalLineItems: lineItems },
      {
        onSuccess: () => {
          lineItems.forEach((item) => {
            setLineItem({
              locationCode: form.locationCode,
              category: "hotel",
              supplierId: item.supplierId,
              supplierRateId: item.supplierRateId,
              autoSelected: false,
            });
          });
          closePanel();
        },
      }
    );
  }

  if (!open) {
    return (
      <div className="mt-6">
        <Button variant="secondary" onClick={() => setOpen(true)}>
          Complete with AI
        </Button>
      </div>
    );
  }

  const suggestion = generate.isSuccess && generate.data.type === "SUGGESTION" ? generate.data : null;
  const noViableSuggestion =
    generate.isSuccess && generate.data.type === "NO_VIABLE_SUGGESTION" ? generate.data : null;

  return (
    <section
      aria-label="ai-assist-panel"
      className="mt-6 rounded-md border border-neutral-300 bg-surface p-4"
    >
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-semibold text-neutral-900">Complete with AI</h2>
        <Button variant="ghost" size="sm" onClick={closePanel}>
          Close
        </Button>
      </div>

      {!generate.isPending && !generate.isSuccess && (
        <form onSubmit={handleSubmit} className="mt-4 space-y-3">
          <div>
            <label htmlFor="ai-location-code" className="mb-1 block text-sm font-medium text-neutral-700">
              Location
            </label>
            <input
              id="ai-location-code"
              required
              value={form.locationCode}
              onChange={(e) => setForm((f) => ({ ...f, locationCode: e.target.value }))}
              className="h-10 w-full rounded-md border border-neutral-300 bg-surface px-3 text-base text-neutral-900"
            />
          </div>
          <div className="flex gap-3">
            <div>
              <label htmlFor="ai-check-in" className="mb-1 block text-sm font-medium text-neutral-700">
                Check-in
              </label>
              <input
                id="ai-check-in"
                type="date"
                required
                value={form.checkIn}
                onChange={(e) => setForm((f) => ({ ...f, checkIn: e.target.value }))}
                className="h-10 rounded-md border border-neutral-300 bg-surface px-3 text-base text-neutral-900"
              />
            </div>
            <div>
              <label htmlFor="ai-check-out" className="mb-1 block text-sm font-medium text-neutral-700">
                Check-out
              </label>
              <input
                id="ai-check-out"
                type="date"
                required
                value={form.checkOut}
                onChange={(e) => setForm((f) => ({ ...f, checkOut: e.target.value }))}
                className="h-10 rounded-md border border-neutral-300 bg-surface px-3 text-base text-neutral-900"
              />
            </div>
          </div>
          <div>
            <label htmlFor="ai-request" className="mb-1 block text-sm font-medium text-neutral-700">
              What is the traveler looking for?
            </label>
            <textarea
              id="ai-request"
              required
              value={form.naturalLanguageRequest}
              onChange={(e) => setForm((f) => ({ ...f, naturalLanguageRequest: e.target.value }))}
              className="w-full rounded-md border border-neutral-300 bg-surface px-3 py-2 text-base text-neutral-900"
              rows={3}
            />
          </div>
          <Button type="submit">Generate suggestions</Button>
        </form>
      )}

      {generate.isPending && (
        <p role="status" className="mt-4 text-sm text-neutral-600">
          Generating AI suggestions…
        </p>
      )}

      {generate.isError && (
        <div
          role="alert"
          className="mt-4 flex items-center justify-between rounded-md border border-error-600/20 bg-error-50 px-4 py-3"
        >
          <p className="text-sm text-error-700">Could not generate AI suggestions.</p>
          <Button variant="secondary" size="sm" onClick={() => generate.mutate(form)}>
            Retry
          </Button>
        </div>
      )}

      {noViableSuggestion && (
        <div className="mt-4 rounded-md border border-neutral-200 px-4 py-3">
          <p className="text-sm text-neutral-700">AI could not find a suggestion: {noViableSuggestion.reason}</p>
          <Button variant="secondary" size="sm" className="mt-2" onClick={closePanel}>
            Dismiss
          </Button>
        </div>
      )}

      {suggestion && (
        <div className="mt-4">
          <ul aria-label="ai-suggested-line-items" className="space-y-2">
            {suggestion.lineItems.map((item) => (
              <li key={item.supplierRateId} className="rounded-md border border-neutral-200 px-3 py-2">
                <p className="text-base text-neutral-900">{item.propertyName}</p>
                <p className="text-sm text-neutral-600">
                  {item.roomType} · {item.netRate.currency} {item.netRate.amount}
                </p>
                <div className="mt-2 flex gap-2">
                  <Badge tone="info">Source: {item.supplierId}</Badge>
                  <Badge tone="neutral">Available as of {new Date(item.availabilityAsOf).toLocaleString()}</Badge>
                </div>
              </li>
            ))}
          </ul>

          {approve.isError && (
            <p role="alert" className="mt-3 text-sm text-error-700">
              Could not save the accepted suggestion.
            </p>
          )}

          <div className="mt-4 flex gap-3">
            <Button
              onClick={() => handleAccept(suggestion.auditLogId, suggestion.lineItems)}
              disabled={approve.isPending}
            >
              Accept
            </Button>
            <Button variant="secondary" onClick={closePanel} disabled={approve.isPending}>
              Reject
            </Button>
          </div>
        </div>
      )}
    </section>
  );
}
