import { useState } from "react";

export interface LocationResult {
  locationCode: string;
  displayName: string;
  latitude: number;
  longitude: number;
  hasInventory: boolean; // PRD 9.4 / 21.1 — zero-inventory locations must still render, distinctly
}

/**
 * State machine backing PRD Section 21.1 (Search Dashboard). Deliberately
 * exposes distinct `idle` / `loading` / `success` / `error` states rather
 * than a single boolean — the empty/error states have different UI
 * treatment per the PRD (Section 9.4 edge cases) and collapsing them into
 * `isLoading` boolean loses that distinction.
 */
export type SearchStatus = "idle" | "loading" | "success" | "error";

export function useMultiLocationSearch() {
  const [status, setStatus] = useState<SearchStatus>("idle");
  const [results, setResults] = useState<LocationResult[]>([]);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  async function search(locationQueries: string[]) {
    if (locationQueries.length === 0) {
      return;
    }
    setStatus("loading");
    setErrorMessage(null);
    try {
      // TODO: replace with a real apiClient.post('/search', ...) call once
      // the backend search endpoint exists (backend/.../booking module).
      const mocked: LocationResult[] = locationQueries.map((q) => ({
        locationCode: q,
        displayName: q,
        latitude: 0,
        longitude: 0,
        hasInventory: true,
      }));
      setResults(mocked);
      setStatus("success");
    } catch (err) {
      setStatus("error");
      setErrorMessage(err instanceof Error ? err.message : "Search failed");
    }
  }

  return { status, results, errorMessage, search };
}
