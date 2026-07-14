import { render, screen, fireEvent, waitFor, within } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { describe, expect, it, vi, beforeEach } from "vitest";
import { ItineraryBuilder } from "./ItineraryBuilder";
import { useItineraryDraftStore } from "./itineraryDraftStore";
import { apiClient } from "@/shared/api/apiClient";

vi.mock("@/shared/api/apiClient", () => ({
  apiClient: { get: vi.fn() },
}));

const ITINERARY_ID = "11111111-1111-1111-1111-111111111111";

function renderWithProviders() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={[`/itinerary/${ITINERARY_ID}`]}>
        <Routes>
          <Route path="/itinerary/:id" element={<ItineraryBuilder />} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>
  );
}

describe("ItineraryBuilder", () => {
  beforeEach(() => {
    vi.mocked(apiClient.get).mockReset();
    useItineraryDraftStore.getState().reset();
  });

  it("default/empty state: shows a message when no locations are in the draft yet", () => {
    renderWithProviders();

    expect(screen.getByText(/no locations yet/i)).toBeInTheDocument();
  });

  it("success state: renders one card per location with the auto-selected badge", () => {
    useItineraryDraftStore.getState().startDraft(ITINERARY_ID);
    useItineraryDraftStore.getState().setLineItem({
      locationCode: "Goa",
      category: "hotel",
      supplierId: "HOTELBEDS",
      supplierRateId: "rate-1",
      autoSelected: true,
    });

    renderWithProviders();

    expect(screen.getByLabelText("itinerary-line-items").children).toHaveLength(1);
    expect(screen.getByText("Goa")).toBeInTheDocument();
    expect(screen.getByText("Auto-selected: Best available match")).toBeInTheDocument();
  });

  it("opens the alternates panel, loads options, and swapping removes the auto-selected badge", async () => {
    useItineraryDraftStore.getState().startDraft(ITINERARY_ID);
    useItineraryDraftStore.getState().setLineItem({
      locationCode: "Goa",
      category: "hotel",
      supplierId: "HOTELBEDS",
      supplierRateId: "rate-1",
      autoSelected: true,
    });
    vi.mocked(apiClient.get).mockResolvedValue({
      data: [
        {
          supplierId: "HOTELBEDS",
          supplierRateId: "rate-1",
          propertyName: "Hotel A",
          roomType: "Deluxe",
          netRateAmount: 5000,
          netRateCurrency: "INR",
          rating: 4.2,
        },
        {
          supplierId: "STUBA",
          supplierRateId: "rate-2",
          propertyName: "Hotel B",
          roomType: "Standard",
          netRateAmount: 3000,
          netRateCurrency: "INR",
          rating: 3.8,
        },
      ],
    });

    renderWithProviders();
    fireEvent.click(screen.getByRole("button", { name: /change/i }));

    await waitFor(() => {
      expect(screen.getByLabelText("alternate-options").children).toHaveLength(2);
    });
    expect(apiClient.get).toHaveBeenCalledWith(
      `/itineraries/${ITINERARY_ID}/alternates`,
      { params: { location: "Goa", category: "hotel" } }
    );

    const hotelBRow = screen.getByText("Hotel B").closest("li");
    if (!hotelBRow) throw new Error("Hotel B row not found");
    fireEvent.click(within(hotelBRow).getByRole("button", { name: /select/i }));

    await waitFor(() => {
      expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
    });
    expect(screen.queryByText("Auto-selected: Best available match")).not.toBeInTheDocument();
    expect(useItineraryDraftStore.getState().lineItems["Goa:hotel"]).toEqual({
      locationCode: "Goa",
      category: "hotel",
      supplierId: "STUBA",
      supplierRateId: "rate-2",
      autoSelected: false,
    });
  });

  it("alternates panel error state: shows a retry option when the fetch fails", async () => {
    useItineraryDraftStore.getState().startDraft(ITINERARY_ID);
    useItineraryDraftStore.getState().setLineItem({
      locationCode: "Goa",
      category: "hotel",
      supplierId: "HOTELBEDS",
      supplierRateId: "rate-1",
      autoSelected: true,
    });
    vi.mocked(apiClient.get).mockRejectedValue(new Error("network error"));

    renderWithProviders();
    fireEvent.click(screen.getByRole("button", { name: /change/i }));

    await waitFor(() => {
      expect(screen.getByRole("alert")).toHaveTextContent(/could not load alternates/i);
    });
    expect(screen.getByRole("button", { name: /retry/i })).toBeInTheDocument();
  });
});
