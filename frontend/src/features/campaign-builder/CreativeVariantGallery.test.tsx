import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { describe, expect, it, vi, beforeEach } from "vitest";
import { CreativeVariantGallery } from "./CreativeVariantGallery";
import { apiClient } from "@/shared/api/apiClient";

vi.mock("@/shared/api/apiClient", () => ({
  apiClient: { get: vi.fn(), post: vi.fn() },
}));

function renderGallery(campaignId = "campaign-1") {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <CreativeVariantGallery campaignId={campaignId} />
    </QueryClientProvider>
  );
}

describe("CreativeVariantGallery", () => {
  beforeEach(() => {
    vi.mocked(apiClient.get).mockReset();
    vi.mocked(apiClient.post).mockReset();
  });

  it("loading state: shows a loading message while fetching persisted variants", () => {
    vi.mocked(apiClient.get).mockReturnValue(new Promise(() => {}));

    renderGallery();

    expect(screen.getByRole("status")).toHaveTextContent(/generating ad creative/i);
  });

  it("error state: shows a retry option when the persisted-variants fetch fails", async () => {
    vi.mocked(apiClient.get).mockRejectedValue(new Error("network error"));

    renderGallery();

    await waitFor(() => {
      expect(screen.getByRole("alert")).toHaveTextContent(/could not load creative variants/i);
    });
    expect(screen.getByRole("button", { name: /retry/i })).toBeInTheDocument();
  });

  it("default state: shows a Generate action when no variants are persisted yet", async () => {
    vi.mocked(apiClient.get).mockResolvedValue({ data: [] });

    renderGallery();

    expect(await screen.findByRole("button", { name: /generate ad creative/i })).toBeInTheDocument();
  });

  it("ADS-04 AC: displays the generated image/copy variant combinations after generation succeeds", async () => {
    vi.mocked(apiClient.get).mockResolvedValue({ data: [] });
    vi.mocked(apiClient.post).mockResolvedValue({
      data: {
        type: "AD_CREATIVE_SUGGESTION",
        auditLogId: "audit-1",
        variants: [
          { headline: "Escape to Goa", bodyText: "Book now at INR 25000" },
          { headline: "Goa Awaits", bodyText: "Sun, sand, and savings" },
        ],
      },
    });
    renderGallery();
    fireEvent.click(await screen.findByRole("button", { name: /generate ad creative/i }));

    await waitFor(() => {
      expect(screen.getByLabelText("creative-variant-gallery").children).toHaveLength(2);
    });
    expect(screen.getByText("Escape to Goa")).toBeInTheDocument();
    expect(screen.getByText("Goa Awaits")).toBeInTheDocument();
  });

  it("empty state: shows AI-05's explicit no-viable-creative outcome, not an error", async () => {
    vi.mocked(apiClient.get).mockResolvedValue({ data: [] });
    vi.mocked(apiClient.post).mockResolvedValue({
      data: { type: "NO_VIABLE_AD_CREATIVE", auditLogId: "audit-1", reason: "No candidate referenced the real price" },
    });
    renderGallery();
    fireEvent.click(await screen.findByRole("button", { name: /generate ad creative/i }));

    await waitFor(() => {
      expect(screen.getByText(/no ai-generated creative could be verified/i)).toBeInTheDocument();
    });
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  });

  it("generation error state: shows an inline error distinct from the no-viable-creative outcome", async () => {
    vi.mocked(apiClient.get).mockResolvedValue({ data: [] });
    vi.mocked(apiClient.post).mockRejectedValue(new Error("network error"));
    renderGallery();
    fireEvent.click(await screen.findByRole("button", { name: /generate ad creative/i }));

    await waitFor(() => {
      expect(screen.getByRole("alert")).toHaveTextContent(/could not generate ad creative/i);
    });
  });

  it("success state: renders already-persisted variants directly, with an Approved badge where set", async () => {
    vi.mocked(apiClient.get).mockResolvedValue({
      data: [
        { variantId: "v1", campaignId: "campaign-1", headline: "Escape to Goa", bodyText: "Book now", imageRef: null, approved: true },
      ],
    });

    renderGallery();

    await waitFor(() => expect(screen.getByText("Escape to Goa")).toBeInTheDocument());
    expect(screen.getByText("Approved")).toBeInTheDocument();
  });
});
