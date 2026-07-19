import { render, screen, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { describe, expect, it, vi, beforeEach } from "vitest";
import { CampaignBillingDetail } from "./CampaignBillingDetail";
import { apiClient } from "@/shared/api/apiClient";

vi.mock("@/shared/api/apiClient", () => ({
  apiClient: { get: vi.fn() },
}));

function renderDetail() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={["/campaigns/campaign-1/billing"]}>
        <Routes>
          <Route path="/campaigns/:campaignId/billing" element={<CampaignBillingDetail />} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>
  );
}

const BILLING_DETAIL = {
  data: {
    campaignId: "campaign-1",
    spendToDateAmount: 150,
    budgetCapAmount: 500,
    budgetCapCurrency: "INR",
    transactions: [
      { transactionId: "txn-1", amount: 50, recordedAt: "2026-07-01T10:00:00Z" },
      { transactionId: "txn-2", amount: 100, recordedAt: "2026-07-02T10:00:00Z" },
    ],
  },
};

describe("CampaignBillingDetail", () => {
  beforeEach(() => {
    vi.mocked(apiClient.get).mockReset();
  });

  it("loading state: shows a loading message while fetching billing detail", () => {
    vi.mocked(apiClient.get).mockReturnValue(new Promise(() => {}));

    renderDetail();

    expect(screen.getByRole("status")).toHaveTextContent(/loading billing detail/i);
  });

  it("error state: shows a retry option when the fetch fails", async () => {
    vi.mocked(apiClient.get).mockRejectedValue(new Error("network error"));

    renderDetail();

    await waitFor(() => {
      expect(screen.getByRole("alert")).toHaveTextContent(/could not load billing detail/i);
    });
  });

  it("empty state: shows a message when no spend has been recorded yet", async () => {
    vi.mocked(apiClient.get).mockResolvedValue({
      data: { ...BILLING_DETAIL.data, spendToDateAmount: 0, transactions: [] },
    });

    renderDetail();

    await waitFor(() => {
      expect(screen.getByText(/no spend has been recorded yet/i)).toBeInTheDocument();
    });
  });

  it("success state: shows spend-to-date, budget cap, and every transaction", async () => {
    vi.mocked(apiClient.get).mockResolvedValue(BILLING_DETAIL);

    renderDetail();

    await waitFor(() => {
      expect(screen.getByLabelText("spend-transactions").children).toHaveLength(2);
    });
    expect(screen.getByText(/150 INR/i)).toBeInTheDocument();
    expect(screen.getByText(/500 INR/i)).toBeInTheDocument();
  });
});
