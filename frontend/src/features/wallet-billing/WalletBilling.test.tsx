import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { describe, expect, it, vi, beforeEach } from "vitest";
import { WalletBilling } from "./WalletBilling";
import { apiClient } from "@/shared/api/apiClient";

vi.mock("@/shared/api/apiClient", () => ({
  apiClient: { get: vi.fn() },
}));

const WALLET_RESPONSE = {
  consultantId: "c1",
  availableBalance: "5000.00",
  creditLimit: "10000.00",
  pendingHolds: "500.00",
  currency: "INR",
  updatedAt: "2026-01-01T00:00:00Z",
};

const EMPTY_LEDGER_RESPONSE = { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 };

function renderWithQueryClient() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <WalletBilling />
    </QueryClientProvider>
  );
}

describe("WalletBilling", () => {
  beforeEach(() => {
    vi.mocked(apiClient.get).mockReset();
  });

  it("loading state: shows loading messages for both the wallet and the ledger", () => {
    vi.mocked(apiClient.get).mockReturnValue(new Promise(() => {}));
    renderWithQueryClient();

    expect(screen.getByText(/loading wallet/i)).toBeInTheDocument();
    expect(screen.getByText(/loading transactions/i)).toBeInTheDocument();
  });

  it("success state: shows the wallet summary and one row per ledger entry", async () => {
    vi.mocked(apiClient.get).mockImplementation((url: string) => {
      if (url === "/wallet") return Promise.resolve({ data: WALLET_RESPONSE });
      return Promise.resolve({
        data: {
          content: [
            {
              ledgerEntryId: "e1",
              consultantId: "c1",
              type: "TOP_UP",
              amount: "1000.00",
              currency: "INR",
              relatedBookingId: null,
              balanceAfter: "1000.00",
              createdAt: "2026-01-01T00:00:00Z",
            },
          ],
          page: 0,
          size: 20,
          totalElements: 1,
          totalPages: 1,
        },
      });
    });
    renderWithQueryClient();

    await waitFor(() => expect(screen.getByText(/5000.00/)).toBeInTheDocument());
    expect(screen.getByLabelText("wallet-ledger").children).toHaveLength(1);
    expect(screen.getByLabelText("wallet-ledger")).toHaveTextContent("TOP_UP");
  });

  it("empty state: shows a message when the ledger has no entries", async () => {
    vi.mocked(apiClient.get).mockImplementation((url: string) => {
      if (url === "/wallet") return Promise.resolve({ data: WALLET_RESPONSE });
      return Promise.resolve({ data: EMPTY_LEDGER_RESPONSE });
    });
    renderWithQueryClient();

    await waitFor(() => expect(screen.getByText(/no transactions yet/i)).toBeInTheDocument());
  });

  it("error state: shows a retry option when the wallet fetch fails", async () => {
    vi.mocked(apiClient.get).mockImplementation((url: string) => {
      if (url === "/wallet") return Promise.reject(new Error("network error"));
      return Promise.resolve({ data: EMPTY_LEDGER_RESPONSE });
    });
    renderWithQueryClient();

    await waitFor(() => {
      expect(screen.getByText(/could not load your wallet/i)).toBeInTheDocument();
    });
  });

  it("filters the ledger by type when a filter is selected (FIN-09 AC)", async () => {
    vi.mocked(apiClient.get).mockImplementation((url: string) => {
      if (url === "/wallet") return Promise.resolve({ data: WALLET_RESPONSE });
      return Promise.resolve({ data: EMPTY_LEDGER_RESPONSE });
    });
    renderWithQueryClient();
    await waitFor(() => expect(screen.getByText(/no transactions yet/i)).toBeInTheDocument());

    fireEvent.change(screen.getByLabelText("filter-ledger-by-type"), { target: { value: "REFUND" } });

    await waitFor(() => {
      expect(apiClient.get).toHaveBeenCalledWith("/wallet/ledger", { params: { type: "REFUND" } });
    });
  });
});
