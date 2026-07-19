import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it, vi, beforeEach } from "vitest";
import { CampaignBuilder } from "./CampaignBuilder";
import { apiClient } from "@/shared/api/apiClient";

vi.mock("@/shared/api/apiClient", () => ({
  apiClient: { get: vi.fn(), post: vi.fn() },
}));

function renderWithProviders(initialEntry = "/campaigns/new") {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={[initialEntry]}>
        <CampaignBuilder />
      </MemoryRouter>
    </QueryClientProvider>
  );
}

const PUBLISHED_PACKAGES = {
  data: {
    content: [
      { packageId: "package-1", name: "Goa Beach Escape", currency: "INR" },
      { packageId: "package-2", name: "Udaipur Heritage Tour", currency: "INR" },
    ],
  },
};

describe("CampaignBuilder", () => {
  beforeEach(() => {
    vi.mocked(apiClient.get).mockReset();
    vi.mocked(apiClient.post).mockReset();
  });

  it("loading state: shows a loading message while fetching published packages", () => {
    vi.mocked(apiClient.get).mockReturnValue(new Promise(() => {}));

    renderWithProviders();

    expect(screen.getByRole("status")).toHaveTextContent(/loading your published packages/i);
  });

  it("error state: shows a retry option when the packages fetch fails", async () => {
    vi.mocked(apiClient.get).mockRejectedValue(new Error("network error"));

    renderWithProviders();

    await waitFor(() => {
      expect(screen.getByRole("alert")).toHaveTextContent(/could not load your published packages/i);
    });
    expect(screen.getByRole("button", { name: /retry/i })).toBeInTheDocument();
  });

  it("empty state: prompts to publish a package first when there are none to promote", async () => {
    vi.mocked(apiClient.get).mockResolvedValue({ data: { content: [] } });

    renderWithProviders();

    await waitFor(() => {
      expect(screen.getByText(/no published packages yet/i)).toBeInTheDocument();
    });
  });

  it("ADS-03 AC: pre-populates the Package selector from ?packageId= when arriving via Package Builder", async () => {
    vi.mocked(apiClient.get).mockResolvedValue(PUBLISHED_PACKAGES);

    renderWithProviders("/campaigns/new?packageId=package-2");

    await waitFor(() => {
      expect(screen.getByLabelText(/^package$/i)).toHaveValue("package-2");
    });
  });

  it("blocks submission and shows validation errors when required fields are missing", async () => {
    vi.mocked(apiClient.get).mockResolvedValue(PUBLISHED_PACKAGES);
    renderWithProviders();
    await waitFor(() => expect(screen.getByLabelText(/^package$/i)).toBeInTheDocument());

    fireEvent.click(screen.getByRole("button", { name: /continue/i }));

    expect(await screen.findByText(/select a package to promote/i)).toBeInTheDocument();
    expect(screen.getByText(/audience description is required/i)).toBeInTheDocument();
    expect(apiClient.post).not.toHaveBeenCalled();
  });

  it("creates the campaign then submits its inputs on the happy path", async () => {
    vi.mocked(apiClient.get).mockResolvedValue(PUBLISHED_PACKAGES);
    vi.mocked(apiClient.post).mockImplementation((url: string) => {
      if (url === "/campaigns") {
        return Promise.resolve({ data: { campaignId: "campaign-1", packageId: "package-1", status: "PENDING_APPROVAL" } });
      }
      if (url.endsWith("/inputs")) {
        return Promise.resolve({ data: { campaignId: "campaign-1", status: "PENDING_APPROVAL" } });
      }
      return Promise.reject(new Error(`unexpected call: ${url}`));
    });
    renderWithProviders("/campaigns/new?packageId=package-1");
    await waitFor(() => expect(screen.getByLabelText(/^package$/i)).toHaveValue("package-1"));

    fireEvent.change(screen.getByLabelText(/audience description/i), { target: { value: "Adults 25-45" } });
    fireEvent.change(screen.getByLabelText(/budget cap/i), { target: { value: "500" } });
    fireEvent.change(screen.getByLabelText(/duration/i), { target: { value: "14" } });
    fireEvent.click(screen.getByRole("button", { name: /continue/i }));

    await waitFor(() => expect(screen.getByText(/campaign started/i)).toBeInTheDocument());
    expect(apiClient.post).toHaveBeenCalledWith("/campaigns", { packageId: "package-1" });
    expect(apiClient.post).toHaveBeenCalledWith("/campaigns/campaign-1/inputs", {
      audienceDescription: "Adults 25-45",
      budgetCapAmount: 500,
      durationDays: 14,
    });
  });
});
