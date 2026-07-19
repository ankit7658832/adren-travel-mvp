import { render, screen, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { describe, expect, it, vi, beforeEach } from "vitest";
import { DisputeTicketTracker } from "./DisputeTicketTracker";
import { apiClient } from "@/shared/api/apiClient";

vi.mock("@/shared/api/apiClient", () => ({
  apiClient: { get: vi.fn() },
}));

function renderWithQueryClient() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <DisputeTicketTracker />
    </QueryClientProvider>
  );
}

describe("DisputeTicketTracker", () => {
  beforeEach(() => {
    vi.mocked(apiClient.get).mockReset();
  });

  it("loading state: shows a loading message while fetching tickets", () => {
    vi.mocked(apiClient.get).mockReturnValue(new Promise(() => {}));
    renderWithQueryClient();

    expect(screen.getByRole("status")).toHaveTextContent(/loading dispute tickets/i);
  });

  it("empty state: shows a message when there are no dispute tickets", async () => {
    vi.mocked(apiClient.get).mockResolvedValue({
      data: { content: [], page: 0, size: 50, totalElements: 0, totalPages: 0 },
    });
    renderWithQueryClient();

    await waitFor(() => {
      expect(screen.getByText(/no dispute tickets/i)).toBeInTheDocument();
    });
  });

  it("success state: renders every ticket with its status", async () => {
    vi.mocked(apiClient.get).mockResolvedValue({
      data: {
        content: [
          {
            disputeTicketId: "dt-1",
            bookingId: "b-1",
            reason: "Wrong room type delivered",
            status: "OPEN",
            createdAt: "2026-07-01T10:00:00Z",
          },
        ],
        page: 0,
        size: 50,
        totalElements: 1,
        totalPages: 1,
      },
    });
    renderWithQueryClient();

    await waitFor(() => {
      expect(screen.getByLabelText("dispute-ticket-list").children).toHaveLength(1);
    });
    expect(screen.getByText("Wrong room type delivered")).toBeInTheDocument();
    expect(screen.getByText("OPEN")).toBeInTheDocument();
  });

  it("error state: shows a retry option when the fetch fails", async () => {
    vi.mocked(apiClient.get).mockRejectedValue(new Error("network error"));
    renderWithQueryClient();

    await waitFor(() => {
      expect(screen.getByRole("alert")).toHaveTextContent(/could not load dispute tickets/i);
    });
    expect(screen.getByRole("button", { name: /retry/i })).toBeInTheDocument();
  });
});
