import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { describe, expect, it, vi, beforeEach } from "vitest";
import { AiAssistPanel } from "./AiAssistPanel";
import { useItineraryDraftStore } from "./itineraryDraftStore";
import { apiClient } from "@/shared/api/apiClient";

vi.mock("@/shared/api/apiClient", () => ({
  apiClient: { post: vi.fn() },
}));

const ITINERARY_ID = "11111111-1111-1111-1111-111111111111";

function renderWithProviders() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <AiAssistPanel itineraryId={ITINERARY_ID} />
    </QueryClientProvider>
  );
}

function openPanelAndFillForm() {
  fireEvent.click(screen.getByRole("button", { name: /complete with ai/i }));
  fireEvent.change(screen.getByLabelText(/location/i), { target: { value: "Goa" } });
  fireEvent.change(screen.getByLabelText(/what is the traveler looking for/i), {
    target: { value: "A relaxing beach trip" },
  });
}

describe("AiAssistPanel", () => {
  beforeEach(() => {
    vi.mocked(apiClient.post).mockReset();
    useItineraryDraftStore.getState().reset();
  });

  it("default state: shows the collapsed Complete with AI entry point", () => {
    renderWithProviders();

    expect(screen.getByRole("button", { name: /complete with ai/i })).toBeInTheDocument();
    expect(screen.queryByLabelText(/location/i)).not.toBeInTheDocument();
  });

  it("shows the request form once opened, and a loading state once submitted", async () => {
    vi.mocked(apiClient.post).mockImplementation(() => new Promise(() => {}));
    renderWithProviders();

    openPanelAndFillForm();
    fireEvent.click(screen.getByRole("button", { name: /generate suggestions/i }));

    await waitFor(() => {
      expect(screen.getByRole("status")).toHaveTextContent(/generating ai suggestions/i);
    });
  });

  it("success state (SUGGESTION): renders source-supplier and availability badges and requires explicit accept", async () => {
    vi.mocked(apiClient.post).mockResolvedValueOnce({
      data: {
        type: "SUGGESTION",
        auditLogId: "audit-1",
        lineItems: [
          {
            supplierId: "HOTELBEDS",
            supplierRateId: "rate-1",
            propertyName: "Taj Palace",
            roomType: "Deluxe Room",
            netRate: { amount: "5000.00", currency: "INR" },
            availabilityAsOf: "2026-07-01T10:00:00Z",
          },
        ],
      },
    });
    renderWithProviders();

    openPanelAndFillForm();
    fireEvent.click(screen.getByRole("button", { name: /generate suggestions/i }));

    await waitFor(() => {
      expect(screen.getByText("Taj Palace")).toBeInTheDocument();
    });
    expect(screen.getByText("Source: HOTELBEDS")).toBeInTheDocument();
    expect(screen.getByText(/available as of/i)).toBeInTheDocument();

    // Nothing is added to the itinerary draft until Accept is clicked.
    expect(useItineraryDraftStore.getState().lineItems).toEqual({});
    expect(screen.getByRole("button", { name: /^accept$/i })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /^reject$/i })).toBeInTheDocument();

    vi.mocked(apiClient.post).mockResolvedValueOnce({ data: undefined });
    fireEvent.click(screen.getByRole("button", { name: /^accept$/i }));

    await waitFor(() => {
      expect(useItineraryDraftStore.getState().lineItems["Goa:hotel"]).toEqual({
        locationCode: "Goa",
        category: "hotel",
        supplierId: "HOTELBEDS",
        supplierRateId: "rate-1",
        autoSelected: false,
      });
    });
    expect(apiClient.post).toHaveBeenCalledWith(`/itineraries/${ITINERARY_ID}/ai-suggestion/approval`, {
      auditLogId: "audit-1",
      finalLineItems: [
        {
          supplierId: "HOTELBEDS",
          supplierRateId: "rate-1",
          propertyName: "Taj Palace",
          roomType: "Deluxe Room",
          netRate: { amount: "5000.00", currency: "INR" },
          availabilityAsOf: "2026-07-01T10:00:00Z",
        },
      ],
    });
  });

  it("rejecting a suggestion never adds it to the itinerary draft", async () => {
    vi.mocked(apiClient.post).mockResolvedValueOnce({
      data: {
        type: "SUGGESTION",
        auditLogId: "audit-1",
        lineItems: [
          {
            supplierId: "HOTELBEDS",
            supplierRateId: "rate-1",
            propertyName: "Taj Palace",
            roomType: "Deluxe Room",
            netRate: { amount: "5000.00", currency: "INR" },
            availabilityAsOf: "2026-07-01T10:00:00Z",
          },
        ],
      },
    });
    renderWithProviders();

    openPanelAndFillForm();
    fireEvent.click(screen.getByRole("button", { name: /generate suggestions/i }));
    await waitFor(() => expect(screen.getByText("Taj Palace")).toBeInTheDocument());

    fireEvent.click(screen.getByRole("button", { name: /^reject$/i }));

    expect(useItineraryDraftStore.getState().lineItems).toEqual({});
    expect(screen.getByRole("button", { name: /complete with ai/i })).toBeInTheDocument();
  });

  it("empty state (NO_VIABLE_SUGGESTION): shows the AI's explicit reason", async () => {
    vi.mocked(apiClient.post).mockResolvedValueOnce({
      data: { type: "NO_VIABLE_SUGGESTION", auditLogId: "audit-2", reason: "No inventory available for Goa" },
    });
    renderWithProviders();

    openPanelAndFillForm();
    fireEvent.click(screen.getByRole("button", { name: /generate suggestions/i }));

    await waitFor(() => {
      expect(screen.getByText(/no inventory available for goa/i)).toBeInTheDocument();
    });
    expect(useItineraryDraftStore.getState().lineItems).toEqual({});
  });

  it("error state: shows a retry option when generation fails", async () => {
    vi.mocked(apiClient.post).mockRejectedValueOnce(new Error("network error"));
    renderWithProviders();

    openPanelAndFillForm();
    fireEvent.click(screen.getByRole("button", { name: /generate suggestions/i }));

    await waitFor(() => {
      expect(screen.getByRole("alert")).toHaveTextContent(/could not generate ai suggestions/i);
    });
    expect(screen.getByRole("button", { name: /retry/i })).toBeInTheDocument();
  });
});
