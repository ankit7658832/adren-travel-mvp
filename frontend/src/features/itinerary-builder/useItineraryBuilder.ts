import { useQuery } from "@tanstack/react-query";
import { apiClient } from "@/shared/api/apiClient";

export interface AlternateOption {
  supplierId: string;
  supplierRateId: string;
  propertyName: string;
  roomType: string;
  netRateAmount: number;
  netRateCurrency: string;
  rating: number | null;
}

/**
 * PRD §21.2 — the alternate-selection side panel's data source (FND-16).
 * Server data (an API-fetched list), so React Query — never copied into
 * the Zustand draft store, which only ever holds the Consultant's current
 * *selection*, not the full catalog of options they chose from (RULES.md
 * §7.1, mirrors {@code itineraryDraftStore.component.test.tsx}'s rule).
 * `enabled: open` means this only fetches once the side panel is actually
 * opened for a location/category, not on every Itinerary Builder render.
 */
export function useAlternates(itineraryId: string, locationCode: string, category: string, open: boolean) {
  return useQuery({
    queryKey: ["itinerary-alternates", itineraryId, locationCode, category],
    queryFn: async () => {
      const { data } = await apiClient.get<AlternateOption[]>(`/itineraries/${itineraryId}/alternates`, {
        params: { location: locationCode, category },
      });
      return data;
    },
    enabled: open,
  });
}
