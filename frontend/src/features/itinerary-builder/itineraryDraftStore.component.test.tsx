import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, beforeEach } from "vitest";
import { useItineraryDraftStore } from "./itineraryDraftStore";

/**
 * A search-result option, shaped like what a React Query-backed search hook
 * would return — deliberately carries fields (price, rating) that must
 * never leak into the Zustand draft store per RULES.md §7.1's rule that
 * server data belongs in React Query's cache, and Zustand only ever holds
 * the minimal cross-cutting selection derived from it.
 */
interface SearchResultOption {
  supplierId: string;
  supplierRateId: string;
  priceAmount: number;
  rating: number;
}

const SEARCH_RESULTS: SearchResultOption[] = [
  { supplierId: "hotelbeds", supplierRateId: "rate-123", priceAmount: 4200, rating: 4.2 },
];

function StepOneResultsPanel() {
  const setLineItem = useItineraryDraftStore((s) => s.setLineItem);
  return (
    <div>
      {SEARCH_RESULTS.map((option) => (
        <button
          key={option.supplierRateId}
          onClick={() =>
            setLineItem({
              locationCode: "GOA",
              category: "hotel",
              supplierId: option.supplierId,
              supplierRateId: option.supplierRateId,
              autoSelected: false,
            })
          }
        >
          Select {option.supplierRateId}
        </button>
      ))}
    </div>
  );
}

describe("itineraryDraftStore never receives a copy of server search-result data", () => {
  beforeEach(() => {
    useItineraryDraftStore.getState().reset();
  });

  it("only stores the minimal derived selection, not the full search-result option", async () => {
    const user = userEvent.setup();
    render(<StepOneResultsPanel />);

    await user.click(screen.getByRole("button", { name: "Select rate-123" }));

    const stored = useItineraryDraftStore.getState().lineItems["GOA:hotel"];
    expect(stored).toEqual({
      locationCode: "GOA",
      category: "hotel",
      supplierId: "hotelbeds",
      supplierRateId: "rate-123",
      autoSelected: false,
    });
    // The full server-shaped option (price, rating) never appears anywhere
    // in the store — only React Query's cache is allowed to hold that.
    expect(stored).not.toHaveProperty("priceAmount");
    expect(stored).not.toHaveProperty("rating");
  });
});
