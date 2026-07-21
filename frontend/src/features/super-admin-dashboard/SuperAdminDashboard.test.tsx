import { render, screen, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { describe, expect, it, vi, beforeEach } from "vitest";
import { SuperAdminDashboard } from "./SuperAdminDashboard";
import { apiClient } from "@/shared/api/apiClient";

vi.mock("@/shared/api/apiClient", () => ({
  apiClient: { get: vi.fn() },
}));

function renderDashboard() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <SuperAdminDashboard />
    </QueryClientProvider>
  );
}

const SUPPLIER_IDS = ["HOTELBEDS", "STUBA", "TBO", "MYSTIFLY", "TRANSFERZ", "WIDGETY", "HBACTIVITIES", "LOCAL_DMC", "BYOS"];

const ACTIVE_REPORT = {
  data: {
    gmv: { gmvByCurrency: [{ currency: "INR", amount: 250000 }] },
    supplierPerformance: SUPPLIER_IDS.map((supplierId) => ({
      supplierId,
      lineItemCount: supplierId === "HOTELBEDS" ? 12 : 0,
    })),
    aiGovernanceSummary: { totalSuggestions: 20, suggestedCount: 15, noViableSuggestionCount: 3, groqErrorCount: 2 },
    adSpend: { spendByCurrency: [{ currency: "INR", amount: 5000 }] },
  },
};

const EMPTY_REPORT = {
  data: {
    gmv: { gmvByCurrency: [] },
    supplierPerformance: SUPPLIER_IDS.map((supplierId) => ({ supplierId, lineItemCount: 0 })),
    aiGovernanceSummary: { totalSuggestions: 0, suggestedCount: 0, noViableSuggestionCount: 0, groqErrorCount: 0 },
    adSpend: { spendByCurrency: [] },
  },
};

describe("SuperAdminDashboard", () => {
  beforeEach(() => {
    vi.mocked(apiClient.get).mockReset();
  });

  it("loading state: shows a loading message while fetching global reporting", () => {
    vi.mocked(apiClient.get).mockReturnValue(new Promise(() => {}));

    renderDashboard();

    expect(screen.getByRole("status")).toHaveTextContent(/loading global reporting/i);
  });

  it("error state: shows a retry option when the fetch fails", async () => {
    vi.mocked(apiClient.get).mockRejectedValue(new Error("network error"));

    renderDashboard();

    await waitFor(() => {
      expect(screen.getByRole("alert")).toHaveTextContent(/could not load global reporting/i);
    });
  });

  it("empty state: shows a message when no platform activity has been recorded", async () => {
    vi.mocked(apiClient.get).mockResolvedValue(EMPTY_REPORT);

    renderDashboard();

    await waitFor(() => {
      expect(screen.getByText(/no platform activity has been recorded yet/i)).toBeInTheDocument();
    });
  });

  it("success state: shows GMV, supplier performance, AI governance summary, and ad spend", async () => {
    vi.mocked(apiClient.get).mockResolvedValue(ACTIVE_REPORT);

    renderDashboard();

    await waitFor(() => {
      expect(screen.getByLabelText("gmv-by-currency")).toBeInTheDocument();
    });
    expect(screen.getByText(/250000 INR/i)).toBeInTheDocument();
    expect(screen.getByLabelText("supplier-performance").children).toHaveLength(9);
    expect(screen.getByText(/20 total suggestions/i)).toBeInTheDocument();
    expect(screen.getByLabelText("ad-spend-by-currency")).toBeInTheDocument();
    expect(screen.getByText(/5000 INR/i)).toBeInTheDocument();
  });
});
