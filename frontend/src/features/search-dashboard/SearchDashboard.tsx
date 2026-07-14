import { useState } from "react";
import { useMultiLocationSearch } from "./useMultiLocationSearch";
import { Button } from "@/shared/design-system/Button";
import { Badge } from "@/shared/design-system/Badge";

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
  const { status, results, errorMessage, search } = useMultiLocationSearch();

  function handleSearch() {
    const locations = locationInput
      .split(",")
      .map((l) => l.trim())
      .filter(Boolean);
    search(locations);
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
        <div
          role="alert"
          className="mt-6 flex items-center justify-between rounded-md border border-error-600/20 bg-error-50 px-4 py-3"
        >
          <p className="text-sm text-error-700">
            {errorMessage ?? "Something went wrong."}
          </p>
          <Button variant="secondary" size="sm" onClick={handleSearch}>
            Retry
          </Button>
        </div>
      )}

      {status === "success" && (
        <ul aria-label="search-results" className="mt-6 space-y-3">
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
        </ul>
      )}
    </main>
  );
}
