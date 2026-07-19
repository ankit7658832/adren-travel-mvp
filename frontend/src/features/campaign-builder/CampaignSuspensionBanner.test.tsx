import { render, screen, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { describe, expect, it, vi, beforeEach } from "vitest";
import { CampaignSuspensionBanner } from "./CampaignSuspensionBanner";
import { apiClient } from "@/shared/api/apiClient";

vi.mock("@/shared/api/apiClient", () => ({
  apiClient: { get: vi.fn() },
}));

function renderBanner() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <CampaignSuspensionBanner campaignId="campaign-1" />
    </QueryClientProvider>
  );
}

describe("CampaignSuspensionBanner", () => {
  beforeEach(() => {
    vi.mocked(apiClient.get).mockReset();
  });

  it("loading state: renders an accessible-only status, no visible flash", () => {
    vi.mocked(apiClient.get).mockReturnValue(new Promise(() => {}));

    renderBanner();

    expect(screen.getByRole("status")).toBeInTheDocument();
    expect(screen.queryByText(/suspended/i)).not.toBeInTheDocument();
  });

  it("error state: shows a quiet inline notice when the status check fails", async () => {
    vi.mocked(apiClient.get).mockRejectedValue(new Error("network error"));

    renderBanner();

    await waitFor(() => {
      expect(screen.getByRole("alert")).toHaveTextContent(/could not check this campaign/i);
    });
  });

  it("empty state: renders nothing when the campaign is not suspended", async () => {
    vi.mocked(apiClient.get).mockResolvedValue({ data: { campaignId: "campaign-1", metaSuspended: false } });

    renderBanner();

    await waitFor(() => {
      expect(apiClient.get).toHaveBeenCalled();
    });
    expect(screen.queryByText(/suspended/i)).not.toBeInTheDocument();
  });

  it("success state: shows the suspended warning banner when the campaign is flagged", async () => {
    vi.mocked(apiClient.get).mockResolvedValue({ data: { campaignId: "campaign-1", metaSuspended: true } });

    renderBanner();

    await waitFor(() => {
      expect(screen.getByRole("alert")).toHaveTextContent(/your meta ad account has been suspended/i);
    });
    expect(screen.getByText(/suspended — action required/i)).toBeInTheDocument();
  });
});
