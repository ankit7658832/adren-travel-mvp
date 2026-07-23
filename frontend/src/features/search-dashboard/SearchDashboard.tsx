import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useMultiLocationSearch } from "./useMultiLocationSearch";
import { MapPanel } from "@/shared/layout/MapPanel";
import { ResultsPanel } from "@/shared/layout/ResultsPanel";
import { Button } from "@/shared/design-system/Button";
import { Badge } from "@/shared/design-system/Badge";
import { ErrorModal } from "@/shared/components/ErrorModal";
import { useItineraryDraftStore } from "@/features/itinerary-builder/itineraryDraftStore";

/**
 * PRD Section 21.1 — Search Dashboard. Layer 1 (Adren product chrome) —
 * doc/DESIGN.md §10. Living reference implementation of the Layer 1 tokens
 * (doc/DESIGN.md §2, §5, §7).
 *
 * States implemented: default (empty box, no results), loading (skeleton),
 * results (per-location cards, including the explicit "no inventory"
 * card per Section 9.4), error (retry prompt, not a blank screen).
 */
export function SearchDashboard() {
  const [locationInput, setLocationInput] = useState("");
  const { status, results, errorMessage, search, dismiss } = useMultiLocationSearch();
  const startDraft = useItineraryDraftStore((s) => s.startDraft);
  const setLineItem = useItineraryDraftStore((s) => s.setLineItem);
  const navigate = useNavigate();

  function handleSearch() {
    const locations = locationInput
      .split(",")
      .map((l) => l.trim())
      .filter(Boolean);
    search(locations);
  }

  // PRD §9.1 Flow A steps 5-6 — hand every location's FND-14 auto-selected
  // default off to the Itinerary Builder (FND-16) via the cross-step
  // Zustand draft store (RULES.md §7.1); Search Dashboard itself never
  // holds this state past the moment of navigation.
  function handleBuildItinerary() {
    const itineraryId = crypto.randomUUID();
    startDraft(itineraryId);
    for (const location of results) {
      if (location.hasInventory && location.autoSelectedSupplierId && location.autoSelectedSupplierRateId) {
        setLineItem({
          locationCode: location.locationCode,
          category: "hotel",
          supplierId: location.autoSelectedSupplierId,
          supplierRateId: location.autoSelectedSupplierRateId,
          autoSelected: true,
          // FES-05: carried through so the Itinerary Builder's own MapPanel
          // can show pins without re-geocoding — a supplier swap later
          // (AlternatesPanel) preserves these, since the location itself
          // doesn't change when the chosen supplier/rate does.
          latitude: location.latitude,
          longitude: location.longitude,
        });
      }
    }
    navigate(`/itinerary/${itineraryId}`);
  }

  return (
    <main className="mx-auto max-w-4xl px-6 py-8">
      <h1 className="text-2xl font-semibold text-neutral-900">
        Search &amp; Build Itinerary
      </h1>

      <div className="mt-6 flex flex-col gap-2 sm:flex-row sm:items-end sm:gap-3">
        <div className="flex-1">
          <label
            htmlFor="location-search"
            className="mb-1 block text-sm font-medium text-neutral-700"
          >
            Locations
          </label>
          <input
            id="location-search"
            placeholder="e.g. Goa, Udaipur, Jaipur"
            value={locationInput}
            onChange={(e) => setLocationInput(e.target.value)}
            className="h-10 w-full rounded-md border border-neutral-300 bg-surface px-3 text-base text-neutral-900 placeholder:text-neutral-500 focus:outline-none focus-visible:ring-2 focus-visible:ring-focus-ring focus-visible:ring-offset-2"
          />
        </div>
        <Button onClick={handleSearch} disabled={status === "loading"}>
          Search
        </Button>
      </div>

      {status === "loading" && (
        <div className="mt-6 space-y-3" aria-hidden="true">
          {[0, 1, 2].map((i) => (
            <div
              key={i}
              className="h-16 animate-pulse rounded-md bg-neutral-100"
            />
          ))}
        </div>
      )}
      {status === "loading" && (
        <p role="status" className="sr-only">
          Loading results…
        </p>
      )}

      {status === "error" && (
        // SCR-16 (doc/ADREN_UIUX_SPEC.md §2.5) — a whole-search failure
        // is exactly the "stops the user's current task" case the Global
        // Error Modal is for, vs. the per-location "no inventory" card
        // (below) used when only some locations fail.
        <ErrorModal
          title="Search failed"
          message={errorMessage ?? "Something went wrong."}
          onRetry={handleSearch}
          onDismiss={dismiss}
        />
      )}

      {status === "success" && (
        <>
          <ResultsPanel
            ariaLabel="search-results"
            map={
              <MapPanel
                pins={results.map((location) => ({
                  id: location.locationCode,
                  latitude: location.latitude,
                  longitude: location.longitude,
                  label: location.displayName,
                  hasInventory: location.hasInventory,
                }))}
                ariaLabel={`Map showing ${results.length} searched location${results.length === 1 ? "" : "s"}`}
              />
            }
          >
            {results.map((location) => (
              <li
                key={location.locationCode}
                className="flex items-center justify-between rounded-md border border-neutral-200 bg-surface px-4 py-3"
              >
                <span className="text-base text-neutral-900">
                  {location.displayName}
                </span>
                {!location.hasInventory && (
                  <Badge tone="neutral">No inventory available</Badge>
                )}
              </li>
            ))}
          </ResultsPanel>
          {results.some((location) => location.hasInventory) && (
            <div className="mt-6 flex justify-end">
              <Button onClick={handleBuildItinerary}>Build Itinerary</Button>
            </div>
          )}
        </>
      )}
    </main>
  );
}
