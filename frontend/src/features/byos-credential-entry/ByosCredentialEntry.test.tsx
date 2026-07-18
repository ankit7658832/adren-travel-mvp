import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { describe, expect, it, vi, beforeEach } from "vitest";
import { ByosCredentialEntry } from "./ByosCredentialEntry";
import { apiClient } from "@/shared/api/apiClient";

vi.mock("@/shared/api/apiClient", () => ({
  apiClient: { get: vi.fn(), post: vi.fn() },
}));

function renderWithQueryClient() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <ByosCredentialEntry />
    </QueryClientProvider>
  );
}

describe("ByosCredentialEntry", () => {
  beforeEach(() => {
    vi.mocked(apiClient.get).mockReset();
    vi.mocked(apiClient.post).mockReset();
  });

  it("loading state: shows a loading message while fetching the list", () => {
    vi.mocked(apiClient.get).mockReturnValue(new Promise(() => {}));
    renderWithQueryClient();

    expect(screen.getByRole("status")).toHaveTextContent(/loading your supplier credentials/i);
  });

  it("empty state: every supplier renders as not-configured when nothing is saved yet", async () => {
    vi.mocked(apiClient.get).mockResolvedValue({ data: [] });
    renderWithQueryClient();

    await waitFor(() => {
      expect(screen.getByLabelText("byos-credential-list").children).toHaveLength(7);
    });
    expect(screen.getAllByText(/not configured/i)).toHaveLength(7);
  });

  it("success state: shows a configured badge for a saved supplier, never the raw secret", async () => {
    vi.mocked(apiClient.get).mockResolvedValue({
      data: [{ supplierId: "HOTELBEDS", configured: true, lastModifiedAt: "2026-01-01T00:00:00Z" }],
    });
    renderWithQueryClient();

    await waitFor(() => {
      expect(screen.getByText(/configured — •••• masked/i)).toBeInTheDocument();
    });
    expect(screen.getAllByText(/not configured/i).length).toBeGreaterThan(0);
  });

  it("error state: shows a retry option when the list fetch fails", async () => {
    vi.mocked(apiClient.get).mockRejectedValue(new Error("network error"));
    renderWithQueryClient();

    await waitFor(() => {
      expect(screen.getByRole("alert")).toHaveTextContent(/could not load your supplier credentials/i);
    });
    expect(screen.getByRole("button", { name: /retry/i })).toBeInTheDocument();
  });

  it("saves a new BYOS credential for the selected supplier, scoped to the caller's own tenant server-side", async () => {
    vi.mocked(apiClient.get).mockResolvedValue({ data: [] });
    vi.mocked(apiClient.post).mockResolvedValue({ data: {} });
    renderWithQueryClient();
    await waitFor(() => expect(screen.getByLabelText("byos-credential-list").children).toHaveLength(7));

    fireEvent.change(screen.getByLabelText(/supplier/i), { target: { value: "STUBA" } });
    fireEvent.change(screen.getByLabelText(/your credential value/i), { target: { value: "consultant-own-secret" } });
    fireEvent.click(screen.getByRole("button", { name: /^save$/i }));

    await waitFor(() => {
      expect(apiClient.post).toHaveBeenCalledWith(
        expect.stringMatching(/^\/consultants\/.+\/byos-credentials$/),
        { supplierId: "STUBA", secretValue: "consultant-own-secret" }
      );
    });
    expect(await screen.findByRole("status")).toHaveTextContent(/credential saved/i);
  });

  it("save error state: shows an inline error", async () => {
    vi.mocked(apiClient.get).mockResolvedValue({ data: [] });
    vi.mocked(apiClient.post).mockRejectedValue(new Error("network error"));
    renderWithQueryClient();
    await waitFor(() => expect(screen.getByLabelText("byos-credential-list").children).toHaveLength(7));

    fireEvent.change(screen.getByLabelText(/your credential value/i), { target: { value: "secret" } });
    fireEvent.click(screen.getByRole("button", { name: /^save$/i }));

    await waitFor(() => {
      expect(screen.getByRole("alert")).toHaveTextContent(/could not save this credential/i);
    });
  });
});
