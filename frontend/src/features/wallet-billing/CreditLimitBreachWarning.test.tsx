import { render, screen, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { describe, expect, it, vi, beforeEach } from "vitest";
import { CreditLimitBreachWarning } from "./CreditLimitBreachWarning";
import { apiClient } from "@/shared/api/apiClient";

vi.mock("@/shared/api/apiClient", () => ({
  apiClient: { get: vi.fn() },
}));

function renderWarning(pendingAmount: number) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <CreditLimitBreachWarning pendingAmount={pendingAmount} />
    </QueryClientProvider>
  );
}

describe("CreditLimitBreachWarning", () => {
  beforeEach(() => {
    vi.mocked(apiClient.get).mockReset();
  });

  it("renders nothing while the wallet is loading", () => {
    vi.mocked(apiClient.get).mockReturnValue(new Promise(() => {}));
    renderWarning(1000);

    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  });

  it("renders nothing when the wallet fetch fails", async () => {
    vi.mocked(apiClient.get).mockRejectedValue(new Error("network error"));
    renderWarning(1000);

    await waitFor(() => expect(apiClient.get).toHaveBeenCalled());
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  });

  it("renders nothing when the pending amount is within available balance plus credit limit", async () => {
    vi.mocked(apiClient.get).mockResolvedValue({
      data: { consultantId: "c1", availableBalance: "1000", creditLimit: "5000", pendingHolds: "0", currency: "INR", updatedAt: "2026-01-01T00:00:00Z" },
    });
    renderWarning(3000);

    await waitFor(() => expect(apiClient.get).toHaveBeenCalled());
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  });

  it("renders an inline warning when the pending amount would breach the credit limit", async () => {
    vi.mocked(apiClient.get).mockResolvedValue({
      data: { consultantId: "c1", availableBalance: "1000", creditLimit: "5000", pendingHolds: "0", currency: "INR", updatedAt: "2026-01-01T00:00:00Z" },
    });
    renderWarning(9000);

    await waitFor(() => {
      expect(screen.getByRole("alert")).toHaveTextContent(/would exceed your credit limit/i);
    });
  });

  it("accounts for pendingHolds already eating into the headroom", async () => {
    vi.mocked(apiClient.get).mockResolvedValue({
      data: { consultantId: "c1", availableBalance: "1000", creditLimit: "5000", pendingHolds: "5500", currency: "INR", updatedAt: "2026-01-01T00:00:00Z" },
    });
    // headroom = 1000 + 5000 - 5500 = 500; pending amount of 600 breaches it.
    renderWarning(600);

    await waitFor(() => {
      expect(screen.getByRole("alert")).toBeInTheDocument();
    });
  });
});
