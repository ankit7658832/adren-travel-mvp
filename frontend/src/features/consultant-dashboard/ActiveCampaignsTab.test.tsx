import { render, screen, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { describe, expect, it, vi, beforeEach } from "vitest";
import { ActiveCampaignsTab } from "./ActiveCampaignsTab";
import { apiClient } from "@/shared/api/apiClient";

vi.mock("@/shared/api/apiClient", () => ({
  apiClient: { get: vi.fn() },
}));

function renderTab() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <ActiveCampaignsTab />
    </QueryClientProvider>
  );
}

const ACTIVE_CAMPAIGNS = {
  data: {
    content: [
      {
        campaignId: "campaign-1",
        packageId: "package-1",
        consultantId: "consultant-1",
        status: "LIVE",
        audienceDescription: "Adults 25-45",
        budgetCapAmount: 500,
        budgetCapCurrency: "INR",
        durationDays: 14,
        metaCampaignRef: "stub-campaign-abc",
        spendToDateAmount: 120,
        rejectionReason: null,
        impressions: 1500,
        clicks: 90,
        bookingsAttributed: 3,
      },
    ],
  },
};

describe("ActiveCampaignsTab", () => {
  beforeEach(() => {
    vi.mocked(apiClient.get).mockReset();
  });

  it("loading state: shows a loading message while fetching campaigns", () => {
    vi.mocked(apiClient.get).mockReturnValue(new Promise(() => {}));

    renderTab();

    expect(screen.getByRole("status")).toHaveTextContent(/loading your campaigns/i);
  });

  it("error state: shows a retry option when the fetch fails", async () => {
    vi.mocked(apiClient.get).mockRejectedValue(new Error("network error"));

    renderTab();

    await waitFor(() => {
      expect(screen.getByRole("alert")).toHaveTextContent(/could not load your campaigns/i);
    });
  });

  it("empty state: shows a message when the consultant has no campaigns yet", async () => {
    vi.mocked(apiClient.get).mockResolvedValue({ data: { content: [] } });

    renderTab();

    await waitFor(() => {
      expect(screen.getByText(/you have no campaigns yet/i)).toBeInTheDocument();
    });
  });

  it("success state: lists each campaign with its performance snapshot", async () => {
    vi.mocked(apiClient.get).mockResolvedValue(ACTIVE_CAMPAIGNS);

    renderTab();

    await waitFor(() => {
      expect(screen.getByLabelText("active-campaigns").children).toHaveLength(1);
    });
    expect(screen.getByText("Adults 25-45")).toBeInTheDocument();
    expect(screen.getByText(/1,500 impressions/i)).toBeInTheDocument();
    expect(screen.getByText(/90 clicks/i)).toBeInTheDocument();
    expect(screen.getByText(/3 bookings attributed/i)).toBeInTheDocument();
  });
});
