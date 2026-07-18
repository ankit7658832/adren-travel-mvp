import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { describe, expect, it, vi, beforeEach } from "vitest";
import { LocalDmcOnboarding } from "./LocalDmcOnboarding";
import { apiClient } from "@/shared/api/apiClient";

vi.mock("@/shared/api/apiClient", () => ({
  apiClient: { get: vi.fn(), post: vi.fn() },
}));

function renderWithProviders() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <LocalDmcOnboarding />
    </QueryClientProvider>
  );
}

describe("LocalDmcOnboarding", () => {
  beforeEach(() => {
    vi.mocked(apiClient.get).mockReset();
    vi.mocked(apiClient.post).mockReset();
  });

  it("loading state: shows a loading message while fetching the list", () => {
    vi.mocked(apiClient.get).mockImplementation(() => new Promise(() => {}));
    renderWithProviders();

    expect(screen.getByRole("status")).toHaveTextContent(/loading local dmcs/i);
  });

  it("empty state: shows a message when no DMCs have been submitted", async () => {
    vi.mocked(apiClient.get).mockResolvedValue({
      data: { content: [], page: 0, size: 50, totalElements: 0, totalPages: 0 },
    });
    renderWithProviders();

    await waitFor(() => {
      expect(screen.getByText(/no local dmcs submitted yet/i)).toBeInTheDocument();
    });
  });

  it("success state: renders every submitted DMC with its status badge", async () => {
    vi.mocked(apiClient.get).mockResolvedValue({
      data: {
        content: [
          {
            localDmcId: "dmc-1",
            consultantId: "consultant-1",
            businessName: "Goa Local Tours",
            productCategories: ["TRANSFER", "ACTIVITY"],
            sampleRatesSummary: "City tour from 2000 INR",
            referencesInfo: "Ref",
            status: "PENDING",
            verificationNotes: null,
            cancellationRate: 0,
            complaintCount: 0,
            flagged: false,
            inventoryStale: false,
            createdAt: "2026-07-01T10:00:00Z",
          },
        ],
        page: 0,
        size: 50,
        totalElements: 1,
        totalPages: 1,
      },
    });
    renderWithProviders();

    await waitFor(() => {
      expect(screen.getByLabelText("local-dmc-list").children).toHaveLength(1);
    });
    expect(screen.getByText("Goa Local Tours")).toBeInTheDocument();
    expect(screen.getByText("TRANSFER, ACTIVITY")).toBeInTheDocument();
    expect(screen.getByText("PENDING")).toBeInTheDocument();
  });

  it("error state: shows a retry option when the list fetch fails", async () => {
    vi.mocked(apiClient.get).mockRejectedValue(new Error("network error"));
    renderWithProviders();

    await waitFor(() => {
      expect(screen.getByRole("alert")).toHaveTextContent(/could not load local dmcs/i);
    });
    expect(screen.getByRole("button", { name: /retry/i })).toBeInTheDocument();
  });

  it("submits a new Local DMC and refreshes the list", async () => {
    vi.mocked(apiClient.get).mockResolvedValue({
      data: { content: [], page: 0, size: 50, totalElements: 0, totalPages: 0 },
    });
    vi.mocked(apiClient.post).mockResolvedValue({ data: { localDmcId: "dmc-new" } });
    renderWithProviders();
    await waitFor(() => expect(screen.getByText(/no local dmcs submitted yet/i)).toBeInTheDocument());

    fireEvent.change(screen.getByLabelText(/business name/i), { target: { value: "Goa Local Tours" } });
    fireEvent.change(screen.getByLabelText(/product categories/i), { target: { value: "TRANSFER, ACTIVITY" } });
    fireEvent.click(screen.getByRole("button", { name: /submit for onboarding/i }));

    await waitFor(() => {
      expect(apiClient.post).toHaveBeenCalledWith("/local-dmc", {
        businessName: "Goa Local Tours",
        productCategories: ["TRANSFER", "ACTIVITY"],
        sampleRatesSummary: "",
        referencesInfo: "",
      });
    });
    // The submit mutation invalidates the list query, triggering a refetch.
    await waitFor(() => expect(apiClient.get).toHaveBeenCalledTimes(2));
  });

  it("submission error state: shows an inline error and keeps the form filled in", async () => {
    vi.mocked(apiClient.get).mockResolvedValue({
      data: { content: [], page: 0, size: 50, totalElements: 0, totalPages: 0 },
    });
    vi.mocked(apiClient.post).mockRejectedValue(new Error("network error"));
    renderWithProviders();
    await waitFor(() => expect(screen.getByText(/no local dmcs submitted yet/i)).toBeInTheDocument());

    fireEvent.change(screen.getByLabelText(/business name/i), { target: { value: "Goa Local Tours" } });
    fireEvent.change(screen.getByLabelText(/product categories/i), { target: { value: "TRANSFER" } });
    fireEvent.click(screen.getByRole("button", { name: /submit for onboarding/i }));

    await waitFor(() => {
      expect(screen.getByRole("alert")).toHaveTextContent(/could not submit this local dmc/i);
    });
    expect(screen.getByLabelText(/business name/i)).toHaveValue("Goa Local Tours");
  });
});
