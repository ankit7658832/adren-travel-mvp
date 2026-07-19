import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { describe, expect, it, vi, beforeEach } from "vitest";
import { PnrBookingSearch } from "./PnrBookingSearch";
import { apiClient } from "@/shared/api/apiClient";

vi.mock("@/shared/api/apiClient", () => ({
  apiClient: { get: vi.fn() },
}));

function renderWithQueryClient() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <PnrBookingSearch />
    </QueryClientProvider>
  );
}

function submitSearch(ref: string) {
  fireEvent.change(screen.getByLabelText(/booking reference/i), { target: { value: ref } });
  fireEvent.click(screen.getByRole("button", { name: /^search$/i }));
}

describe("PnrBookingSearch", () => {
  beforeEach(() => {
    vi.mocked(apiClient.get).mockReset();
  });

  it("default state: the search button is present with no results shown yet", () => {
    renderWithQueryClient();

    expect(screen.getByRole("button", { name: /^search$/i })).toBeInTheDocument();
    expect(screen.queryByLabelText("booking-search-results")).not.toBeInTheDocument();
  });

  it("loading state: shows a searching message while the request is in flight", () => {
    vi.mocked(apiClient.get).mockReturnValue(new Promise(() => {}));
    renderWithQueryClient();

    submitSearch("ABCD1234");

    expect(screen.getByRole("status")).toHaveTextContent(/searching/i);
  });

  it("success state: shows a booking summary regardless of product type, with a click-through to full detail", async () => {
    vi.mocked(apiClient.get).mockResolvedValue({
      data: {
        content: [
          {
            bookingId: "b-1",
            pnrSearchableRef: "ABCD1234",
            status: "CONFIRMED",
            totalSellPrice: { amount: "11500.00", currency: "INR" },
            paymentMethod: "WALLET",
            createdAt: "2026-07-01T10:00:00Z",
          },
        ],
        page: 0,
        size: 20,
        totalElements: 1,
        totalPages: 1,
      },
    });
    renderWithQueryClient();

    submitSearch("ABCD1234");

    await waitFor(() => {
      expect(screen.getByLabelText("booking-search-results").children).toHaveLength(1);
    });
    expect(screen.getByText("ABCD1234")).toBeInTheDocument();
    expect(screen.getByText("CONFIRMED")).toBeInTheDocument();
    expect(apiClient.get).toHaveBeenCalledWith("/bookings/search", { params: { ref: "ABCD1234" } });

    // Click-through reveals full detail.
    expect(screen.queryByText("b-1")).not.toBeInTheDocument();
    fireEvent.click(screen.getByText("ABCD1234"));
    expect(screen.getByText("b-1")).toBeInTheDocument();
    expect(screen.getByText("WALLET")).toBeInTheDocument();
  });

  it("empty state: shows a message when no booking matches the reference", async () => {
    vi.mocked(apiClient.get).mockResolvedValue({
      data: { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 },
    });
    renderWithQueryClient();

    submitSearch("NOSUCH99");

    await waitFor(() => {
      expect(screen.getByText(/no booking found/i)).toBeInTheDocument();
    });
  });

  it("error state: shows a retry option when the search fails", async () => {
    vi.mocked(apiClient.get).mockRejectedValue(new Error("network error"));
    renderWithQueryClient();

    submitSearch("ABCD1234");

    await waitFor(() => {
      expect(screen.getByRole("alert")).toHaveTextContent(/could not search for this booking/i);
    });
    expect(screen.getByRole("button", { name: /retry/i })).toBeInTheDocument();
  });
});
