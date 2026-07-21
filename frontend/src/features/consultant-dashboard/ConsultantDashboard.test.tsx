import { render, screen, waitFor, fireEvent } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { describe, expect, it, vi, beforeEach } from "vitest";
import { ConsultantDashboard } from "./ConsultantDashboard";
import { apiClient } from "@/shared/api/apiClient";

vi.mock("@/shared/api/apiClient", () => ({
  apiClient: { get: vi.fn() },
}));

function renderDashboard() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <ConsultantDashboard />
    </QueryClientProvider>
  );
}

const ACTIVE_DASHBOARD = {
  data: {
    metrics: { bookingsThisMonth: 4, gmvThisMonth: { amount: 84000, currency: "INR" } },
    wallet: { consultantId: "c-1", availableBalance: 50000, creditLimit: 100000, pendingHolds: 0, currency: "INR", updatedAt: "2026-07-01T00:00:00Z" },
    topPackages: [{ packageId: "pkg-1", name: "Goa Escape", bookingCount: 3 }],
    pendingQuotations: [{ itineraryId: "itin-1", createdAt: "2026-07-01T00:00:00Z" }],
    activeCampaigns: [],
  },
};

const EMPTY_DASHBOARD = {
  data: {
    metrics: { bookingsThisMonth: 0, gmvThisMonth: { amount: 0, currency: "INR" } },
    wallet: { consultantId: "c-1", availableBalance: 0, creditLimit: 100000, pendingHolds: 0, currency: "INR", updatedAt: "2026-07-01T00:00:00Z" },
    topPackages: [],
    pendingQuotations: [],
    activeCampaigns: [],
  },
};

describe("ConsultantDashboard", () => {
  beforeEach(() => {
    vi.mocked(apiClient.get).mockReset();
  });

  it("loading state: shows a loading message while fetching the dashboard", () => {
    vi.mocked(apiClient.get).mockReturnValue(new Promise(() => {}));

    renderDashboard();

    expect(screen.getByRole("status")).toHaveTextContent(/loading your dashboard/i);
  });

  it("error state: shows a retry option when the fetch fails", async () => {
    vi.mocked(apiClient.get).mockRejectedValue(new Error("network error"));

    renderDashboard();

    await waitFor(() => {
      expect(screen.getByRole("alert")).toHaveTextContent(/could not load your dashboard/i);
    });
  });

  it("HRD-10 empty state: shows the onboarding checklist, not zeroed-out charts, for a new Consultant with zero bookings", async () => {
    vi.mocked(apiClient.get).mockResolvedValue(EMPTY_DASHBOARD);

    renderDashboard();

    await waitFor(() => {
      expect(screen.getByLabelText("onboarding-checklist")).toBeInTheDocument();
    });
    expect(screen.queryByLabelText("dashboard-tabs")).not.toBeInTheDocument();
    expect(screen.queryByText(/bookings this month/i)).not.toBeInTheDocument();
  });

  it("success state: shows summary cards and the Top Packages tab by default", async () => {
    vi.mocked(apiClient.get).mockResolvedValue(ACTIVE_DASHBOARD);

    renderDashboard();

    await waitFor(() => {
      expect(screen.getByLabelText("top-packages")).toBeInTheDocument();
    });
    expect(screen.getByText("4")).toBeInTheDocument();
    expect(screen.getByText(/84000 INR/i)).toBeInTheDocument();
    expect(screen.getByText("Goa Escape")).toBeInTheDocument();
  });

  it("success state: switches to the Pending Quotations tab on click", async () => {
    vi.mocked(apiClient.get).mockResolvedValue(ACTIVE_DASHBOARD);

    renderDashboard();
    await waitFor(() => expect(screen.getByLabelText("top-packages")).toBeInTheDocument());

    fireEvent.click(screen.getByRole("tab", { name: /pending quotations/i }));

    expect(screen.getByLabelText("pending-quotations")).toBeInTheDocument();
  });
});
