import { describe, expect, it, beforeEach } from "vitest";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter } from "react-router-dom";
import { http, HttpResponse } from "msw";
import { SearchDashboard } from "./SearchDashboard";
import { server } from "@/test/mswServer";
import { useItineraryDraftStore } from "@/features/itinerary-builder/itineraryDraftStore";

function renderWithQueryClient() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <SearchDashboard />
      </MemoryRouter>
    </QueryClientProvider>
  );
}

let capturedRequestBody: unknown;

function mockSearchResponse(locationCodes: string[]) {
  server.use(
    http.post("/api/v1/search", async ({ request }) => {
      capturedRequestBody = await request.json();
      return HttpResponse.json({
        locations: locationCodes.map((code) => ({
          locationCode: code,
          displayName: code,
          latitude: 20,
          longitude: 80,
          hasInventory: true,
          autoSelectedSupplierId: "HOTELBEDS",
          autoSelectedSupplierRateId: `rate-${code}`,
        })),
      });
    }),
  );
}

/**
 * Component-level integration test — renders the real component tree
 * (hook + component together), asserting on user-visible behavior rather
 * than implementation details. Traces to PRD Section 22.1 acceptance
 * criteria ("multi-location search... every location, even if one has no
 * inventory").
 * <p>
 * TST-04 — MSW intercepts the real HTTP call apiClient makes instead of
 * mocking the apiClient module (see src/test/mswServer.ts).
 */
describe("SearchDashboard", () => {
  beforeEach(() => {
    useItineraryDraftStore.getState().reset();
  });

  it("shows a loading state immediately after search is submitted", async () => {
    // React Query's mutation status flips to "pending" via its notify
    // manager, which batches on a microtask rather than synchronously in
    // the click handler (unlike the old hand-rolled useState version) — a
    // handler that never resolves lets the test observe "loading" without
    // racing a near-instant mocked resolution; the test ends (and MSW
    // resets handlers) before it would ever need to.
    server.use(http.post("/api/v1/search", () => new Promise<Response>(() => {})));
    renderWithQueryClient();

    fireEvent.change(screen.getByLabelText(/locations/i), {
      target: { value: "Goa, Udaipur" },
    });
    fireEvent.click(screen.getByRole("button", { name: /search/i }));

    await waitFor(() => expect(screen.getByRole("status")).toHaveTextContent(/loading/i));
  });

  it("renders one result item per submitted location", async () => {
    mockSearchResponse(["Goa", "Udaipur", "Jaipur"]);
    renderWithQueryClient();

    fireEvent.change(screen.getByLabelText(/locations/i), {
      target: { value: "Goa, Udaipur, Jaipur" },
    });
    fireEvent.click(screen.getByRole("button", { name: /search/i }));

    await waitFor(() => {
      expect(screen.getByLabelText("search-results").children).toHaveLength(3);
    });
  });

  it("ignores blank/whitespace-only location entries", async () => {
    mockSearchResponse(["Goa", "Udaipur"]);
    renderWithQueryClient();

    fireEvent.change(screen.getByLabelText(/locations/i), {
      target: { value: "Goa, , Udaipur" },
    });
    fireEvent.click(screen.getByRole("button", { name: /search/i }));

    await waitFor(() => {
      expect(screen.getByLabelText("search-results").children).toHaveLength(2);
    });
    expect(capturedRequestBody).toEqual({ locationQueries: ["Goa", "Udaipur"] });
  });

  it("renders a map pin for a location with no inventory, distinctly (T1)", async () => {
    server.use(
      http.post("/api/v1/search", () =>
        HttpResponse.json({
          locations: [
            { locationCode: "Goa", displayName: "Goa", latitude: 15, longitude: 74, hasInventory: true },
            { locationCode: "Antarctica", displayName: "Antarctica", latitude: 10, longitude: 90, hasInventory: false },
          ],
        }),
      ),
    );
    renderWithQueryClient();

    fireEvent.change(screen.getByLabelText(/locations/i), {
      target: { value: "Goa, Antarctica" },
    });
    fireEvent.click(screen.getByRole("button", { name: /search/i }));

    await waitFor(() => {
      expect(screen.getAllByTestId("map-pin")).toHaveLength(2);
    });
    expect(screen.getByText("No inventory available")).toBeInTheDocument();
  });

  it("seeds the itinerary draft store with each location's auto-selected line item on Build Itinerary", async () => {
    mockSearchResponse(["Goa", "Udaipur"]);
    renderWithQueryClient();

    fireEvent.change(screen.getByLabelText(/locations/i), {
      target: { value: "Goa, Udaipur" },
    });
    fireEvent.click(screen.getByRole("button", { name: /search/i }));

    await waitFor(() => {
      expect(screen.getByRole("button", { name: /build itinerary/i })).toBeInTheDocument();
    });
    fireEvent.click(screen.getByRole("button", { name: /build itinerary/i }));

    const draft = useItineraryDraftStore.getState();
    expect(draft.itineraryId).not.toBeNull();
    expect(draft.lineItems["Goa:hotel"]).toEqual({
      locationCode: "Goa",
      category: "hotel",
      supplierId: "HOTELBEDS",
      supplierRateId: "rate-Goa",
      autoSelected: true,
      // FES-05: carried through from the search result so the Itinerary
      // Builder's own MapPanel doesn't need to re-geocode.
      latitude: 20,
      longitude: 80,
    });
    expect(draft.lineItems["Udaipur:hotel"]).toBeDefined();
  });

  it("does not offer Build Itinerary when no location has inventory", async () => {
    server.use(
      http.post("/api/v1/search", () =>
        HttpResponse.json({
          locations: [
            {
              locationCode: "Antarctica", displayName: "Antarctica", latitude: 10, longitude: 90, hasInventory: false,
              autoSelectedSupplierId: null, autoSelectedSupplierRateId: null,
            },
          ],
        }),
      ),
    );
    renderWithQueryClient();

    fireEvent.change(screen.getByLabelText(/locations/i), { target: { value: "Antarctica" } });
    fireEvent.click(screen.getByRole("button", { name: /search/i }));

    await waitFor(() => {
      expect(screen.getByText("No inventory available")).toBeInTheDocument();
    });
    expect(screen.queryByRole("button", { name: /build itinerary/i })).not.toBeInTheDocument();
  });

  it("shows an error state when the search request fails", async () => {
    server.use(http.post("/api/v1/search", () => HttpResponse.json({ message: "Search failed" }, { status: 500 })));
    renderWithQueryClient();

    fireEvent.change(screen.getByLabelText(/locations/i), {
      target: { value: "Goa" },
    });
    fireEvent.click(screen.getByRole("button", { name: /search/i }));

    await waitFor(() => {
      expect(screen.getByRole("alert")).toHaveTextContent(/search failed|request failed/i);
    });
  });
});
