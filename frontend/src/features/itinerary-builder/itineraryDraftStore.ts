/**
 * doc/architecture/RULES.md §7.1's state-management boundary, established
 * deliberately here (FES-03) rather than by the first feature that happens
 * to need cross-cutting client state under time pressure: the in-progress
 * itinerary-builder draft (per-location, per-category line-item selections
 * spanning the multi-step wizard, PRD §21.2) is genuinely cross-cutting
 * client state that outlives any single wizard-step component, so it lives
 * in Zustand — never in React Query (that's for server-fetched data, e.g.
 * search results) and never in a step component's local `useState` (which
 * unmounts and loses state when the Consultant moves to another step).
 */
import { create } from "zustand";

export interface ItineraryLineItem {
  locationCode: string;
  category: string;
  supplierId: string;
  supplierRateId: string;
  /** Surfaced via FND-15's "Auto-selected: Best available match" badge. */
  autoSelected: boolean;
  /** FES-05 — carried from Search Dashboard's hand-off so the Itinerary Builder's own MapPanel doesn't need to re-geocode; absent for line items that never passed through that hand-off. */
  latitude?: number;
  longitude?: number;
}

function lineItemKey(locationCode: string, category: string): string {
  return `${locationCode}:${category}`;
}

interface ItineraryDraftStore {
  itineraryId: string | null;
  lineItems: Record<string, ItineraryLineItem>;
  startDraft: (itineraryId: string) => void;
  setLineItem: (item: ItineraryLineItem) => void;
  removeLineItem: (locationCode: string, category: string) => void;
  reset: () => void;
}

const initialState = {
  itineraryId: null,
  lineItems: {},
};

export const useItineraryDraftStore = create<ItineraryDraftStore>((set) => ({
  ...initialState,

  startDraft: (itineraryId) => set({ itineraryId, lineItems: {} }),

  setLineItem: (item) =>
    set((state) => ({
      lineItems: {
        ...state.lineItems,
        [lineItemKey(item.locationCode, item.category)]: item,
      },
    })),

  removeLineItem: (locationCode, category) =>
    set((state) => {
      const next = { ...state.lineItems };
      delete next[lineItemKey(locationCode, category)];
      return { lineItems: next };
    }),

  reset: () => set(initialState),
}));
