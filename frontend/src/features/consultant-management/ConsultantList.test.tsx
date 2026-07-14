import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { describe, expect, it, vi, beforeEach } from "vitest";
import { ConsultantList } from "./ConsultantList";
import { apiClient } from "@/shared/api/apiClient";

vi.mock("@/shared/api/apiClient", () => ({
  apiClient: { get: vi.fn(), patch: vi.fn() },
}));

function renderWithQueryClient() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <ConsultantList />
    </QueryClientProvider>
  );
}

describe("ConsultantList", () => {
  beforeEach(() => {
    vi.mocked(apiClient.get).mockReset();
    vi.mocked(apiClient.patch).mockReset();
  });

  it("loading state: shows a loading message while fetching consultants", () => {
    vi.mocked(apiClient.get).mockReturnValue(new Promise(() => {}));
    renderWithQueryClient();

    expect(screen.getByRole("status")).toHaveTextContent(/loading consultants/i);
  });

  it("empty state: shows a message when there are no consultants yet", async () => {
    vi.mocked(apiClient.get).mockResolvedValue({ data: { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 } });
    renderWithQueryClient();

    await waitFor(() => {
      expect(screen.getByText(/no consultants onboarded yet/i)).toBeInTheDocument();
    });
  });

  it("success state: renders one row per consultant, with status and home market", async () => {
    vi.mocked(apiClient.get).mockResolvedValue({
      data: {
        content: [
          {
            consultantId: "c1",
            businessName: "Test Travel Co",
            homeMarket: "INDIA",
            status: "ACTIVE",
            createdAt: "2026-01-01T00:00:00Z",
          },
        ],
        page: 0,
        size: 20,
        totalElements: 1,
        totalPages: 1,
      },
    });
    renderWithQueryClient();

    await waitFor(() => {
      expect(screen.getByLabelText("consultant-list").children).toHaveLength(1);
    });
    expect(screen.getByText("Test Travel Co")).toBeInTheDocument();
    expect(screen.getByText("ACTIVE")).toBeInTheDocument();
  });

  it("error state: shows a retry option when the consultants fetch fails", async () => {
    vi.mocked(apiClient.get).mockRejectedValue(new Error("network error"));
    renderWithQueryClient();

    await waitFor(() => {
      expect(screen.getByRole("alert")).toHaveTextContent(/could not load consultants/i);
    });
    expect(screen.getByRole("button", { name: /retry/i })).toBeInTheDocument();
  });

  it("suspends an active consultant via the status action button", async () => {
    vi.mocked(apiClient.get).mockResolvedValue({
      data: {
        content: [
          {
            consultantId: "c1",
            businessName: "Test Travel Co",
            homeMarket: "INDIA",
            status: "ACTIVE",
            createdAt: "2026-01-01T00:00:00Z",
          },
        ],
        page: 0,
        size: 20,
        totalElements: 1,
        totalPages: 1,
      },
    });
    vi.mocked(apiClient.patch).mockResolvedValue({ data: undefined });
    renderWithQueryClient();

    await waitFor(() => {
      expect(screen.getByRole("button", { name: /suspend/i })).toBeInTheDocument();
    });
    fireEvent.click(screen.getByRole("button", { name: /suspend/i }));

    await waitFor(() => {
      expect(apiClient.patch).toHaveBeenCalledWith("/consultants/c1/status", { status: "SUSPENDED" });
    });
  });

  it("reinstates a suspended consultant via the status action button", async () => {
    vi.mocked(apiClient.get).mockResolvedValue({
      data: {
        content: [
          {
            consultantId: "c1",
            businessName: "Test Travel Co",
            homeMarket: "INDIA",
            status: "SUSPENDED",
            createdAt: "2026-01-01T00:00:00Z",
          },
        ],
        page: 0,
        size: 20,
        totalElements: 1,
        totalPages: 1,
      },
    });
    vi.mocked(apiClient.patch).mockResolvedValue({ data: undefined });
    renderWithQueryClient();

    await waitFor(() => {
      expect(screen.getByRole("button", { name: /reinstate/i })).toBeInTheDocument();
    });
    fireEvent.click(screen.getByRole("button", { name: /reinstate/i }));

    await waitFor(() => {
      expect(apiClient.patch).toHaveBeenCalledWith("/consultants/c1/status", { status: "ACTIVE" });
    });
  });
});
