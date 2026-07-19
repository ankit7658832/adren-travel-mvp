import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { describe, expect, it, vi, beforeEach } from "vitest";
import { CampaignPolicyReviewQueue } from "./CampaignPolicyReviewQueue";
import { apiClient } from "@/shared/api/apiClient";

vi.mock("@/shared/api/apiClient", () => ({
  apiClient: { get: vi.fn(), post: vi.fn() },
}));

function renderQueue() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <CampaignPolicyReviewQueue />
    </QueryClientProvider>
  );
}

const PENDING_CAMPAIGNS = {
  data: {
    content: [
      {
        campaignId: "campaign-1",
        packageId: "package-1",
        consultantId: "consultant-1",
        status: "PENDING_POLICY_REVIEW",
        audienceDescription: "Adults 25-45",
        budgetCapAmount: 500,
        budgetCapCurrency: "INR",
        durationDays: 14,
      },
    ],
  },
};

describe("CampaignPolicyReviewQueue", () => {
  beforeEach(() => {
    vi.mocked(apiClient.get).mockReset();
    vi.mocked(apiClient.post).mockReset();
  });

  it("loading state: shows a loading message while fetching the queue", () => {
    vi.mocked(apiClient.get).mockReturnValue(new Promise(() => {}));

    renderQueue();

    expect(screen.getByRole("status")).toHaveTextContent(/loading the review queue/i);
  });

  it("error state: shows a retry option when the queue fetch fails", async () => {
    vi.mocked(apiClient.get).mockRejectedValue(new Error("network error"));

    renderQueue();

    await waitFor(() => {
      expect(screen.getByRole("alert")).toHaveTextContent(/could not load the review queue/i);
    });
  });

  it("empty state: shows a message when nothing is pending review", async () => {
    vi.mocked(apiClient.get).mockResolvedValue({ data: { content: [] } });

    renderQueue();

    await waitFor(() => {
      expect(screen.getByText(/no campaigns are currently pending policy review/i)).toBeInTheDocument();
    });
  });

  it("success state: lists each pending campaign", async () => {
    vi.mocked(apiClient.get).mockResolvedValue(PENDING_CAMPAIGNS);

    renderQueue();

    await waitFor(() => {
      expect(screen.getByLabelText("policy-review-queue").children).toHaveLength(1);
    });
    expect(screen.getByText("Adults 25-45")).toBeInTheDocument();
  });

  it("ADS-06 AC #2: rejecting a campaign requires a reason and surfaces it via the reject endpoint", async () => {
    vi.mocked(apiClient.get).mockResolvedValue(PENDING_CAMPAIGNS);
    vi.mocked(apiClient.post).mockResolvedValue({ data: {} });
    renderQueue();
    await waitFor(() => expect(screen.getByText("Adults 25-45")).toBeInTheDocument());

    // Blocked without a reason.
    expect(screen.getByRole("button", { name: /reject/i })).toBeDisabled();

    fireEvent.change(screen.getByLabelText(/rejection reason/i), { target: { value: "Unverified pricing claim" } });
    expect(screen.getByRole("button", { name: /reject/i })).toBeEnabled();
    fireEvent.click(screen.getByRole("button", { name: /reject/i }));

    await waitFor(() => {
      expect(apiClient.post).toHaveBeenCalledWith("/campaigns/campaign-1/policy-review", {
        reason: "Unverified pricing claim",
      });
    });
  });
});
