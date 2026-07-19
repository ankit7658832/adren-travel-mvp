import { render, screen, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { describe, expect, it, vi } from "vitest";
import { useTenantBranding } from "./useTenantBranding";
import { resolveTenantTheme } from "./resolveTenantTheme";
import { TenantThemedSurface } from "./TenantThemedSurface";
import { apiClient } from "@/shared/api/apiClient";

vi.mock("@/shared/api/apiClient", () => ({
  apiClient: { get: vi.fn() },
}));

function TestStorefront({ consultantId }: { consultantId: string }) {
  const brandingQuery = useTenantBranding(consultantId);

  if (brandingQuery.isLoading) {
    return (
      <p role="status" data-testid="loading">
        Loading branding…
      </p>
    );
  }
  if (brandingQuery.isError || !brandingQuery.data) {
    return (
      <p role="alert" data-testid="error">
        Could not load branding.
      </p>
    );
  }

  const theme = resolveTenantTheme(brandingQuery.data, {
    header: brandingQuery.data.backgroundColor,
    hero: brandingQuery.data.backgroundColor,
  });

  return (
    <TenantThemedSurface theme={theme}>
      <span>{theme.consultantName}</span>
    </TenantThemedSurface>
  );
}

function renderWithQueryClient(consultantId: string) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <TestStorefront consultantId={consultantId} />
    </QueryClientProvider>
  );
}

describe("useTenantBranding (FES-06 runtime theme provider)", () => {
  it("fetches the real branding profile and sets CSS custom properties from the loaded data, not a build-time value", async () => {
    vi.mocked(apiClient.get).mockResolvedValue({
      data: {
        consultantId: "c1",
        logoUrl: "https://example.com/logo.png",
        backgroundImageUrl: null,
        backgroundColor: "#123456",
        textColorPrimary: "#ffffff",
        textColorSecondary: "#eeeeee",
        domain: "mytravel.example.com",
        updatedAt: "2026-01-01T00:00:00Z",
      },
    });

    renderWithQueryClient("c1");

    await waitFor(() => expect(screen.getByText("mytravel.example.com")).toBeInTheDocument());
    expect(apiClient.get).toHaveBeenCalledWith("/consultants/c1/branding");

    const surface = document.querySelector("[data-tenant-theme]");
    expect(surface).not.toBeNull();
    expect(surface).toHaveStyle({ "--tenant-bg-color": "#123456" });
    expect(surface).toHaveStyle({ "--tenant-logo-url": "url(https://example.com/logo.png)" });
  });

  it("shows a loading state while the branding profile is being fetched", () => {
    vi.mocked(apiClient.get).mockReturnValue(new Promise(() => {}));

    renderWithQueryClient("c1");

    expect(screen.getByTestId("loading")).toBeInTheDocument();
  });

  it("shows an error state, not a crash, when the branding fetch fails", async () => {
    vi.mocked(apiClient.get).mockRejectedValue(new Error("network error"));

    renderWithQueryClient("c1");

    await waitFor(() => expect(screen.getByTestId("error")).toBeInTheDocument());
  });
});
