import { useMutation } from "@tanstack/react-query";
import { apiClient } from "@/shared/api/apiClient";

export interface LocationResult {
  locationCode: string;
  displayName: string;
  latitude: number;
  longitude: number;
  hasInventory: boolean; // PRD 9.4 / 21.1 — zero-inventory locations must still render, distinctly
  /** FND-14's Default Selection Algorithm's output for this location — null when there's no inventory. */
  autoSelectedSupplierId: string | null;
  autoSelectedSupplierRateId: string | null;
}

interface SearchResponseDto {
  locations: LocationResult[];
}

/**
 * State machine backing PRD Section 21.1 (Search Dashboard). Deliberately
 * exposes distinct `idle` / `loading` / `success` / `error` states rather
 * than a single boolean — the empty/error states have different UI
 * treatment per the PRD (Section 9.4 edge cases) and collapsing them into
 * `isLoading` boolean loses that distinction.
 */
export type SearchStatus = "idle" | "loading" | "success" | "error";

/**
 * FND-13: search is triggered by explicit user action (not an automatic
 * re-fetch on mount/param change), so this is a `useMutation`, not a
 * `useQuery` — per RULES.md §7.1's reconciliation note. The hook's returned
 * shape (status/results/errorMessage/search) is unchanged from the mocked
 * version specifically so `SearchDashboard.tsx` didn't need to change when
 * this landed.
 */
export function useMultiLocationSearch() {
  const mutation = useMutation({
    mutationFn: async (locationQueries: string[]) => {
      const { data } = await apiClient.post<SearchResponseDto>("/search", { locationQueries });
      return data.locations;
    },
  });

  function search(locationQueries: string[]) {
    if (locationQueries.length === 0) {
      return;
    }
    mutation.mutate(locationQueries);
  }

  const status: SearchStatus = mutation.isIdle
    ? "idle"
    : mutation.isPending
      ? "loading"
      : mutation.isError
        ? "error"
        : "success";

  return {
    status,
    results: mutation.data ?? [],
    errorMessage: mutation.error instanceof Error ? mutation.error.message : null,
    search,
    // SCR-16 — the Global Error Modal's Dismiss action clears the whole-
    // search-failed state back to idle, rather than leaving the modal's
    // trigger condition permanently true.
    dismiss: mutation.reset,
  };
}
