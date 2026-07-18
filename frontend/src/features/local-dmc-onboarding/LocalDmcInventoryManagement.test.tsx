import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { describe, expect, it, vi, beforeEach } from "vitest";
import { LocalDmcInventoryManagement } from "./LocalDmcInventoryManagement";
import { apiClient } from "@/shared/api/apiClient";

vi.mock("@/shared/api/apiClient", () => ({
  apiClient: { get: vi.fn(), patch: vi.fn() },
}));

const LOCAL_DMC_ID = "11111111-1111-1111-1111-111111111111";
const ITEM_ID = "22222222-2222-2222-2222-222222222222";

function renderWithProviders() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={[`/local-dmc/${LOCAL_DMC_ID}/inventory`]}>
        <Routes>
          <Route path="/local-dmc/:id/inventory" element={<LocalDmcInventoryManagement />} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>
  );
}

const SAMPLE_ITEM = {
  itemId: ITEM_ID,
  localDmcId: LOCAL_DMC_ID,
  productName: "City Tour",
  category: "ACTIVITY",
  netRate: 2000,
  netRateCurrency: "INR",
  cancellationPolicyText: "Free cancellation",
  availableFrom: "2026-08-01",
  availableTo: "2026-12-31",
  updatedAt: "2026-07-01T10:00:00Z",
};

describe("LocalDmcInventoryManagement", () => {
  beforeEach(() => {
    vi.mocked(apiClient.get).mockReset();
    vi.mocked(apiClient.patch).mockReset();
  });

  it("loading state: shows a loading message while fetching the list", () => {
    vi.mocked(apiClient.get).mockImplementation(() => new Promise(() => {}));
    renderWithProviders();

    expect(screen.getByRole("status")).toHaveTextContent(/loading inventory/i);
  });

  it("empty state: shows a message when no inventory items have been uploaded", async () => {
    vi.mocked(apiClient.get).mockResolvedValue({
      data: { content: [], page: 0, size: 50, totalElements: 0, totalPages: 0 },
    });
    renderWithProviders();

    await waitFor(() => {
      expect(screen.getByText(/no inventory items uploaded yet/i)).toBeInTheDocument();
    });
  });

  it("success state: renders every inventory item", async () => {
    vi.mocked(apiClient.get).mockResolvedValue({
      data: { content: [SAMPLE_ITEM], page: 0, size: 50, totalElements: 1, totalPages: 1 },
    });
    renderWithProviders();

    await waitFor(() => {
      expect(screen.getByLabelText("local-dmc-inventory-list").children).toHaveLength(1);
    });
    expect(screen.getByText("City Tour")).toBeInTheDocument();
    expect(screen.getByText(/ACTIVITY · 2000 INR/i)).toBeInTheDocument();
  });

  it("error state: shows a retry option when the list fetch fails", async () => {
    vi.mocked(apiClient.get).mockRejectedValue(new Error("network error"));
    renderWithProviders();

    await waitFor(() => {
      expect(screen.getByRole("alert")).toHaveTextContent(/could not load inventory items/i);
    });
    expect(screen.getByRole("button", { name: /retry/i })).toBeInTheDocument();
  });

  it("edits an inventory item and saves the changes", async () => {
    vi.mocked(apiClient.get).mockResolvedValue({
      data: { content: [SAMPLE_ITEM], page: 0, size: 50, totalElements: 1, totalPages: 1 },
    });
    vi.mocked(apiClient.patch).mockResolvedValue({ data: {} });
    renderWithProviders();
    await waitFor(() => expect(screen.getByText("City Tour")).toBeInTheDocument());

    fireEvent.click(screen.getByRole("button", { name: /edit/i }));
    const nameInput = await screen.findByLabelText(/product name/i);
    fireEvent.change(nameInput, { target: { value: "City Tour (revised)" } });
    fireEvent.change(screen.getByLabelText(/net rate/i), { target: { value: "2500" } });
    fireEvent.click(screen.getByRole("button", { name: /save/i }));

    await waitFor(() => {
      expect(apiClient.patch).toHaveBeenCalledWith(`/local-dmc/${LOCAL_DMC_ID}/inventory/${ITEM_ID}`, {
        productName: "City Tour (revised)",
        category: "ACTIVITY",
        netRate: 2500,
        netRateCurrency: "INR",
        cancellationPolicyText: "Free cancellation",
        availableFrom: "2026-08-01",
        availableTo: "2026-12-31",
      });
    });
  });

  it("save error state: shows an inline error and keeps the form open", async () => {
    vi.mocked(apiClient.get).mockResolvedValue({
      data: { content: [SAMPLE_ITEM], page: 0, size: 50, totalElements: 1, totalPages: 1 },
    });
    vi.mocked(apiClient.patch).mockRejectedValue(new Error("network error"));
    renderWithProviders();
    await waitFor(() => expect(screen.getByText("City Tour")).toBeInTheDocument());

    fireEvent.click(screen.getByRole("button", { name: /edit/i }));
    await screen.findByLabelText(/product name/i);
    fireEvent.click(screen.getByRole("button", { name: /save/i }));

    await waitFor(() => {
      expect(screen.getByRole("alert")).toHaveTextContent(/could not save this inventory item/i);
    });
    expect(screen.getByLabelText(/product name/i)).toHaveValue("City Tour");
  });

  it("cancels editing without saving", async () => {
    vi.mocked(apiClient.get).mockResolvedValue({
      data: { content: [SAMPLE_ITEM], page: 0, size: 50, totalElements: 1, totalPages: 1 },
    });
    renderWithProviders();
    await waitFor(() => expect(screen.getByText("City Tour")).toBeInTheDocument());

    fireEvent.click(screen.getByRole("button", { name: /edit/i }));
    await screen.findByLabelText(/product name/i);
    fireEvent.click(screen.getByRole("button", { name: /cancel/i }));

    expect(screen.queryByLabelText(/product name/i)).not.toBeInTheDocument();
    expect(apiClient.patch).not.toHaveBeenCalled();
  });
});
