import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { describe, expect, it, vi, beforeEach } from "vitest";
import { NotificationPreferences } from "./NotificationPreferences";
import { apiClient } from "@/shared/api/apiClient";

vi.mock("@/shared/api/apiClient", () => ({
  apiClient: { get: vi.fn(), put: vi.fn() },
}));

function renderWithQueryClient() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <NotificationPreferences />
    </QueryClientProvider>
  );
}

describe("NotificationPreferences", () => {
  beforeEach(() => {
    vi.mocked(apiClient.get).mockReset();
    vi.mocked(apiClient.put).mockReset();
  });

  it("loading state: shows a loading message while fetching the preference", () => {
    vi.mocked(apiClient.get).mockReturnValue(new Promise(() => {}));
    renderWithQueryClient();

    expect(screen.getByRole("status")).toHaveTextContent(/loading your notification preferences/i);
  });

  it("success state: pre-selects the regional default and labels it as such", async () => {
    vi.mocked(apiClient.get).mockResolvedValue({ data: { secondaryChannel: "WHATSAPP", isOverride: false } });
    renderWithQueryClient();

    await waitFor(() => {
      expect(screen.getByLabelText(/secondary channel/i)).toHaveValue("WHATSAPP");
    });
    expect(screen.getByText(/regional default/i)).toBeInTheDocument();
  });

  it("success state: shows a saved override as custom, pre-selected", async () => {
    vi.mocked(apiClient.get).mockResolvedValue({ data: { secondaryChannel: "SMS", isOverride: true } });
    renderWithQueryClient();

    await waitFor(() => {
      expect(screen.getByLabelText(/secondary channel/i)).toHaveValue("SMS");
    });
    expect(screen.getByText(/custom/i)).toBeInTheDocument();
  });

  it("error state: shows a retry option when the fetch fails", async () => {
    vi.mocked(apiClient.get).mockRejectedValue(new Error("network error"));
    renderWithQueryClient();

    await waitFor(() => {
      expect(screen.getByRole("alert")).toHaveTextContent(/could not load your notification preferences/i);
    });
    expect(screen.getByRole("button", { name: /retry/i })).toBeInTheDocument();
  });

  it("overriding the default and saving calls the API with the new channel, scoped server-side to the caller's own tenant", async () => {
    vi.mocked(apiClient.get).mockResolvedValue({ data: { secondaryChannel: "WHATSAPP", isOverride: false } });
    vi.mocked(apiClient.put).mockResolvedValue({ data: {} });
    renderWithQueryClient();
    await waitFor(() => expect(screen.getByLabelText(/secondary channel/i)).toHaveValue("WHATSAPP"));

    fireEvent.change(screen.getByLabelText(/secondary channel/i), { target: { value: "SMS" } });
    fireEvent.click(screen.getByRole("button", { name: /^save$/i }));

    await waitFor(() => {
      expect(apiClient.put).toHaveBeenCalledWith(
        expect.stringMatching(/^\/consultants\/.+\/notification-preference$/),
        { secondaryChannel: "SMS" }
      );
    });
    expect(await screen.findByText(/preference saved/i)).toBeInTheDocument();
  });

  it("save error state: shows an inline error", async () => {
    vi.mocked(apiClient.get).mockResolvedValue({ data: { secondaryChannel: "WHATSAPP", isOverride: false } });
    vi.mocked(apiClient.put).mockRejectedValue(new Error("network error"));
    renderWithQueryClient();
    await waitFor(() => expect(screen.getByLabelText(/secondary channel/i)).toHaveValue("WHATSAPP"));

    fireEvent.click(screen.getByRole("button", { name: /^save$/i }));

    await waitFor(() => {
      expect(screen.getByRole("alert")).toHaveTextContent(/could not save this preference/i);
    });
  });
});
