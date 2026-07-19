import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { describe, expect, it, vi, beforeEach } from "vitest";
import { CreativeApprovalPanel } from "./CreativeApprovalPanel";
import { apiClient } from "@/shared/api/apiClient";

vi.mock("@/shared/api/apiClient", () => ({
  apiClient: { get: vi.fn(), post: vi.fn() },
}));

function renderPanel(campaignId = "campaign-1") {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <CreativeApprovalPanel campaignId={campaignId} />
    </QueryClientProvider>
  );
}

const UNAPPROVED_VARIANTS = {
  data: [
    { variantId: "v1", campaignId: "campaign-1", headline: "Escape to Goa", bodyText: "Book now", imageRef: null, approved: false },
    { variantId: "v2", campaignId: "campaign-1", headline: "Goa Awaits", bodyText: "Sun and sand", imageRef: null, approved: false },
  ],
};

describe("CreativeApprovalPanel", () => {
  beforeEach(() => {
    vi.mocked(apiClient.get).mockReset();
    vi.mocked(apiClient.post).mockReset();
  });

  it("loading state: shows a loading message while fetching variants", () => {
    vi.mocked(apiClient.get).mockReturnValue(new Promise(() => {}));

    renderPanel();

    expect(screen.getByRole("status")).toHaveTextContent(/loading creative variants/i);
  });

  it("error state: shows a retry option when the variants fetch fails", async () => {
    vi.mocked(apiClient.get).mockRejectedValue(new Error("network error"));

    renderPanel();

    await waitFor(() => {
      expect(screen.getByRole("alert")).toHaveTextContent(/could not load creative variants/i);
    });
  });

  it("empty state: prompts to generate creative first when there is nothing to approve", async () => {
    vi.mocked(apiClient.get).mockResolvedValue({ data: [] });

    renderPanel();

    await waitFor(() => {
      expect(screen.getByText(/generate ad creative first/i)).toBeInTheDocument();
    });
  });

  it("ADS-05 AC: blocks submission (shows the not-yet-ready message) until every variant is approved", async () => {
    vi.mocked(apiClient.get).mockResolvedValue(UNAPPROVED_VARIANTS);

    renderPanel();

    await waitFor(() => {
      expect(screen.getByRole("alert")).toHaveTextContent(/0 of 2 variants approved/i);
    });
    expect(screen.getByRole("alert")).toHaveTextContent(/every variant must be approved/i);
  });

  it("approving one variant leaves the panel still blocked until the rest are also approved", async () => {
    vi.mocked(apiClient.get).mockResolvedValue(UNAPPROVED_VARIANTS);
    vi.mocked(apiClient.post).mockResolvedValue({ data: { ...UNAPPROVED_VARIANTS.data[0], approved: true } });
    renderPanel();
    await waitFor(() => expect(screen.getByLabelText("Escape to Goa")).toBeInTheDocument());

    fireEvent.click(screen.getByLabelText("Escape to Goa"));

    await waitFor(() => expect(apiClient.post).toHaveBeenCalledWith("/campaigns/campaign-1/creative-variants/v1/approval"));
  });

  it("shows the ready-to-submit status once every variant carries an approval", async () => {
    vi.mocked(apiClient.get).mockResolvedValue({
      data: UNAPPROVED_VARIANTS.data.map((v) => ({ ...v, approved: true })),
    });

    renderPanel();

    await waitFor(() => {
      expect(screen.getByText(/all variants approved — ready to submit/i)).toBeInTheDocument();
    });
  });
});
