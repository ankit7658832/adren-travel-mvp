import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { describe, expect, it, vi, beforeEach } from "vitest";
import { SupplierCredentialManagement } from "./SupplierCredentialManagement";
import { apiClient } from "@/shared/api/apiClient";

vi.mock("@/shared/api/apiClient", () => ({
  apiClient: { get: vi.fn(), put: vi.fn() },
}));

function renderWithQueryClient() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <SupplierCredentialManagement />
    </QueryClientProvider>
  );
}

describe("SupplierCredentialManagement", () => {
  beforeEach(() => {
    vi.mocked(apiClient.get).mockReset();
    vi.mocked(apiClient.put).mockReset();
  });

  it("loading state", () => {
    vi.mocked(apiClient.get).mockReturnValue(new Promise(() => {}));
    renderWithQueryClient();

    expect(screen.getByRole("status")).toHaveTextContent(/loading supplier credentials/i);
  });

  it("success state: shows configured vs not-configured badges, never a raw secret", async () => {
    vi.mocked(apiClient.get).mockResolvedValue({
      data: [{ supplierId: "HOTELBEDS", configured: true, lastModifiedByUserId: "u1", lastModifiedAt: "2026-01-01T00:00:00Z" }],
    });
    renderWithQueryClient();

    await waitFor(() => {
      expect(screen.getByLabelText("supplier-credential-list").children).toHaveLength(7);
    });
    expect(screen.getByText(/configured — •••• masked/i)).toBeInTheDocument();
    expect(screen.getAllByText(/not configured/i).length).toBeGreaterThan(0);
  });

  it("error state: shows a retry option", async () => {
    vi.mocked(apiClient.get).mockRejectedValue(new Error("network error"));
    renderWithQueryClient();

    await waitFor(() => {
      expect(screen.getByRole("alert")).toHaveTextContent(/could not load supplier credentials/i);
    });
  });

  it("saves a new credential value for the selected supplier", async () => {
    vi.mocked(apiClient.get).mockResolvedValue({ data: [] });
    vi.mocked(apiClient.put).mockResolvedValue({ data: {} });
    renderWithQueryClient();

    fireEvent.change(screen.getByLabelText(/supplier/i), { target: { value: "STUBA" } });
    fireEvent.change(screen.getByLabelText(/new credential value/i), { target: { value: "secret-xyz" } });
    fireEvent.click(screen.getByRole("button", { name: /^save$/i }));

    await waitFor(() => {
      expect(apiClient.put).toHaveBeenCalledWith("/suppliers/STUBA/credentials", { secretValue: "secret-xyz" });
    });
    expect(await screen.findByRole("status")).toHaveTextContent(/credential updated/i);
  });
});
