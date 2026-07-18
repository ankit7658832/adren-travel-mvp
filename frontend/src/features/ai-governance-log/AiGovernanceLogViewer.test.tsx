import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { describe, expect, it, vi, beforeEach } from "vitest";
import { AiGovernanceLogViewer } from "./AiGovernanceLogViewer";
import { apiClient } from "@/shared/api/apiClient";

vi.mock("@/shared/api/apiClient", () => ({
  apiClient: { get: vi.fn() },
}));

function renderWithProviders() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <AiGovernanceLogViewer />
    </QueryClientProvider>
  );
}

const SAMPLE_ENTRY = {
  auditLogId: "audit-1",
  correlationId: "corr-1",
  attemptNumber: 1,
  consultantId: "consultant-1",
  itineraryId: "itinerary-1",
  requestInputJson: '{"locationCode":"GOA"}',
  sourceDataSnapshotJson: "[]",
  aiOutputJson: '{"selectedSupplierRateIds":["rate-1"],"viable":true}',
  disposition: "SUGGESTED",
  createdAt: "2026-07-01T10:00:00Z",
};

describe("AiGovernanceLogViewer", () => {
  beforeEach(() => {
    vi.mocked(apiClient.get).mockReset();
  });

  it("loading state: shows a loading message while fetching", () => {
    vi.mocked(apiClient.get).mockImplementation(() => new Promise(() => {}));
    renderWithProviders();

    expect(screen.getByRole("status")).toHaveTextContent(/loading ai governance logs/i);
  });

  it("empty state: shows a message when no entries exist", async () => {
    vi.mocked(apiClient.get).mockResolvedValue({
      data: { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 },
    });
    renderWithProviders();

    await waitFor(() => {
      expect(screen.getByText(/no ai suggestions logged yet/i)).toBeInTheDocument();
    });
  });

  it("success state: renders every entry's input/source data/output/disposition", async () => {
    vi.mocked(apiClient.get).mockResolvedValue({
      data: { content: [SAMPLE_ENTRY], page: 0, size: 20, totalElements: 1, totalPages: 1 },
    });
    renderWithProviders();

    await waitFor(() => {
      expect(screen.getByLabelText("ai-governance-log-entries").children).toHaveLength(1);
    });
    expect(screen.getByText("SUGGESTED")).toBeInTheDocument();
    expect(screen.getByText(/consultant-1/)).toBeInTheDocument();
    expect(screen.getByText('{"locationCode":"GOA"}')).toBeInTheDocument();
    expect(screen.getByText('{"selectedSupplierRateIds":["rate-1"],"viable":true}')).toBeInTheDocument();
  });

  it("error state: shows a retry option when the fetch fails", async () => {
    vi.mocked(apiClient.get).mockRejectedValue(new Error("network error"));
    renderWithProviders();

    await waitFor(() => {
      expect(screen.getByRole("alert")).toHaveTextContent(/could not load ai governance logs/i);
    });
    expect(screen.getByRole("button", { name: /retry/i })).toBeInTheDocument();
  });

  it("filters by consultant ID, resetting to the first page", async () => {
    vi.mocked(apiClient.get).mockResolvedValue({
      data: { content: [SAMPLE_ENTRY], page: 0, size: 20, totalElements: 1, totalPages: 1 },
    });
    renderWithProviders();
    await waitFor(() => expect(apiClient.get).toHaveBeenCalled());

    fireEvent.change(screen.getByLabelText(/filter by consultant id/i), { target: { value: "consultant-1" } });

    await waitFor(() => {
      expect(apiClient.get).toHaveBeenCalledWith(
        "/ai/audit-log",
        { params: { consultantId: "consultant-1", page: 0, size: 20 } }
      );
    });
  });

  it("paginates using Next/Previous, disabled at the bounds", async () => {
    vi.mocked(apiClient.get).mockResolvedValue({
      data: { content: [SAMPLE_ENTRY], page: 0, size: 20, totalElements: 40, totalPages: 2 },
    });
    renderWithProviders();
    await waitFor(() => expect(screen.getByText(/page 1 of 2/i)).toBeInTheDocument());

    expect(screen.getByRole("button", { name: /previous/i })).toBeDisabled();
    expect(screen.getByRole("button", { name: /^next$/i })).toBeEnabled();

    vi.mocked(apiClient.get).mockResolvedValue({
      data: { content: [SAMPLE_ENTRY], page: 1, size: 20, totalElements: 40, totalPages: 2 },
    });
    fireEvent.click(screen.getByRole("button", { name: /^next$/i }));

    await waitFor(() => {
      expect(apiClient.get).toHaveBeenCalledWith(
        "/ai/audit-log",
        { params: { consultantId: undefined, page: 1, size: 20 } }
      );
    });
    await waitFor(() => expect(screen.getByText(/page 2 of 2/i)).toBeInTheDocument());
    expect(screen.getByRole("button", { name: /^next$/i })).toBeDisabled();
  });
});
