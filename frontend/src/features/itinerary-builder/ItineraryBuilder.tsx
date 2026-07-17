import { useMemo, useState } from "react";
import { useParams } from "react-router-dom";
import { Badge } from "@/shared/design-system/Badge";
import { Button } from "@/shared/design-system/Button";
import { AiAssistPanel } from "./AiAssistPanel";
import { useItineraryDraftStore, type ItineraryLineItem } from "./itineraryDraftStore";
import { useAlternates, type AlternateOption } from "./useItineraryBuilder";

type SortField = "price" | "rating" | "supplier";

/**
 * PRD §21.2 — the Itinerary Builder: one card per location's current line
 * item (seeded from Search Dashboard's FND-14 auto-selection via the
 * Zustand draft store, RULES.md §7.1), each with a "Change" action that
 * opens a side panel of alternates to swap to (FND-16).
 */
export function ItineraryBuilder() {
  const { id: itineraryId } = useParams<{ id: string }>();
  const lineItems = useItineraryDraftStore((s) => s.lineItems);
  const [activePanel, setActivePanel] = useState<{ locationCode: string; category: string } | null>(null);

  const items = Object.values(lineItems);

  return (
    <main className="mx-auto max-w-3xl px-6 py-8">
      <h1 className="text-2xl font-semibold text-neutral-900">Itinerary Builder</h1>

      {items.length === 0 && (
        <p className="mt-6 text-sm text-neutral-600">
          No locations yet — search for destinations and select "Build Itinerary" to start.
        </p>
      )}

      {items.length > 0 && (
        <ul aria-label="itinerary-line-items" className="mt-6 space-y-3">
          {items.map((item) => (
            <li
              key={`${item.locationCode}:${item.category}`}
              className="flex items-center justify-between rounded-md border border-neutral-200 bg-surface px-4 py-3"
            >
              <div>
                <p className="text-base text-neutral-900">{item.locationCode}</p>
                <p className="text-sm text-neutral-600">
                  {item.supplierId} · {item.supplierRateId}
                </p>
              </div>
              <div className="flex items-center gap-3">
                {item.autoSelected && <Badge tone="info">Auto-selected: Best available match</Badge>}
                <Button
                  variant="secondary"
                  size="sm"
                  onClick={() => setActivePanel({ locationCode: item.locationCode, category: item.category })}
                >
                  Change
                </Button>
              </div>
            </li>
          ))}
        </ul>
      )}

      {activePanel && itineraryId && (
        <AlternatesPanel
          itineraryId={itineraryId}
          locationCode={activePanel.locationCode}
          category={activePanel.category}
          onClose={() => setActivePanel(null)}
        />
      )}

      {itineraryId && <AiAssistPanel itineraryId={itineraryId} />}
    </main>
  );
}

interface AlternatesPanelProps {
  itineraryId: string;
  locationCode: string;
  category: string;
  onClose: () => void;
}

function AlternatesPanel({ itineraryId, locationCode, category, onClose }: AlternatesPanelProps) {
  const [sortField, setSortField] = useState<SortField>("price");
  const [supplierFilter, setSupplierFilter] = useState<string>("");
  const setLineItem = useItineraryDraftStore((s) => s.setLineItem);
  const alternatesQuery = useAlternates(itineraryId, locationCode, category, true);

  const suppliers = useMemo(
    () => Array.from(new Set((alternatesQuery.data ?? []).map((a) => a.supplierId))),
    [alternatesQuery.data]
  );

  const sortedAlternates = useMemo(() => {
    const alternates = (alternatesQuery.data ?? []).filter(
      (a) => !supplierFilter || a.supplierId === supplierFilter
    );
    return [...alternates].sort((a, b) => {
      if (sortField === "price") return a.netRateAmount - b.netRateAmount;
      if (sortField === "rating") return (b.rating ?? 0) - (a.rating ?? 0);
      return a.supplierId.localeCompare(b.supplierId);
    });
  }, [alternatesQuery.data, sortField, supplierFilter]);

  function handleSelect(alternate: AlternateOption) {
    const lineItem: ItineraryLineItem = {
      locationCode,
      category,
      supplierId: alternate.supplierId,
      supplierRateId: alternate.supplierRateId,
      autoSelected: false,
    };
    setLineItem(lineItem);
    onClose();
  }

  return (
    <div role="dialog" aria-label={`alternates-panel-${locationCode}`} className="mt-6 rounded-md border border-neutral-300 bg-surface p-4">
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-semibold text-neutral-900">Alternates for {locationCode}</h2>
        <Button variant="ghost" size="sm" onClick={onClose}>
          Close
        </Button>
      </div>

      <div className="mt-4 flex gap-3">
        <div>
          <label htmlFor="sort-field" className="mb-1 block text-sm font-medium text-neutral-700">
            Sort by
          </label>
          <select
            id="sort-field"
            value={sortField}
            onChange={(e) => setSortField(e.target.value as SortField)}
            className="h-10 rounded-md border border-neutral-300 bg-surface px-3 text-base text-neutral-900"
          >
            <option value="price">Price</option>
            <option value="rating">Rating</option>
            <option value="supplier">Supplier</option>
          </select>
        </div>
        <div>
          <label htmlFor="supplier-filter" className="mb-1 block text-sm font-medium text-neutral-700">
            Supplier
          </label>
          <select
            id="supplier-filter"
            value={supplierFilter}
            onChange={(e) => setSupplierFilter(e.target.value)}
            className="h-10 rounded-md border border-neutral-300 bg-surface px-3 text-base text-neutral-900"
          >
            <option value="">All suppliers</option>
            {suppliers.map((supplier) => (
              <option key={supplier} value={supplier}>
                {supplier}
              </option>
            ))}
          </select>
        </div>
      </div>

      <div className="mt-4">
        {alternatesQuery.isLoading && (
          <p role="status" className="text-sm text-neutral-600">
            Loading alternates…
          </p>
        )}

        {alternatesQuery.isError && (
          <div role="alert" className="flex items-center justify-between rounded-md border border-error-600/20 bg-error-50 px-4 py-3">
            <p className="text-sm text-error-700">Could not load alternates.</p>
            <Button variant="secondary" size="sm" onClick={() => alternatesQuery.refetch()}>
              Retry
            </Button>
          </div>
        )}

        {alternatesQuery.isSuccess && sortedAlternates.length === 0 && (
          <p className="text-sm text-neutral-600">No alternates available.</p>
        )}

        {alternatesQuery.isSuccess && sortedAlternates.length > 0 && (
          <ul aria-label="alternate-options" className="space-y-2">
            {sortedAlternates.map((alternate) => (
              <li
                key={`${alternate.supplierId}:${alternate.supplierRateId}`}
                className="flex items-center justify-between rounded-md border border-neutral-200 px-3 py-2"
              >
                <div>
                  <p className="text-base text-neutral-900">{alternate.propertyName}</p>
                  <p className="text-sm text-neutral-600">
                    {alternate.supplierId} · {alternate.roomType} · {alternate.netRateAmount} {alternate.netRateCurrency}
                    {alternate.rating != null ? ` · ${alternate.rating}★` : ""}
                  </p>
                </div>
                <Button size="sm" onClick={() => handleSelect(alternate)}>
                  Select
                </Button>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  );
}
