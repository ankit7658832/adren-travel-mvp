import { useState } from "react";
import { useMultiLocationSearch } from "./useMultiLocationSearch";

/**
 * PRD Section 21.1 — Search Dashboard.
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
    <main>
      <h1>Search &amp; Build Itinerary</h1>

      <label htmlFor="location-search">Locations</label>
      <input
        id="location-search"
        placeholder="e.g. Goa, Udaipur, Jaipur"
        value={locationInput}
        onChange={(e) => setLocationInput(e.target.value)}
      />
      <button onClick={handleSearch} disabled={status === "loading"}>
        Search
      </button>

      {status === "loading" && <p role="status">Loading results…</p>}

      {status === "error" && (
        <div role="alert">
          <p>{errorMessage ?? "Something went wrong."}</p>
          <button onClick={handleSearch}>Retry</button>
        </div>
      )}

      {status === "success" && (
        <ul aria-label="search-results">
          {results.map((location) => (
            <li key={location.locationCode}>
              {location.hasInventory ? (
                <span>{location.displayName}</span>
              ) : (
                <span>{location.displayName} — No inventory available</span>
              )}
            </li>
          ))}
        </ul>
      )}
    </main>
  );
}
