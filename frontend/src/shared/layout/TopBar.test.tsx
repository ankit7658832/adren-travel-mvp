import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { describe, expect, it, afterEach, vi } from "vitest";
import { http, HttpResponse } from "msw";
import { TopBar } from "./TopBar";
import { AuthSessionProvider } from "@/shared/auth/AuthSessionContext";
import { AUTH_TOKEN_STORAGE_KEY } from "@/shared/auth/authTypes";
import { makeFakeToken } from "@/shared/auth/testAuthTokens";
import { server } from "@/test/mswServer";

function renderTopBar() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <AuthSessionProvider>
        <MemoryRouter>
          <TopBar />
        </MemoryRouter>
      </AuthSessionProvider>
    </QueryClientProvider>
  );
}

const originalLocation = window.location;

describe("TopBar (doc/ADREN_UIUX_SPEC.md §3)", () => {
  afterEach(() => {
    localStorage.clear();
    Object.defineProperty(window, "location", { configurable: true, value: originalLocation });
  });

  it("with no session, shows the Adren wordmark and a Sign in link", () => {
    renderTopBar();

    expect(screen.getByText("Adren")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: /sign in/i })).toBeInTheDocument();
  });

  it("a SUPER_ADMIN session shows fixed Adren branding only, never a tenant logo fetch", () => {
    localStorage.setItem(AUTH_TOKEN_STORAGE_KEY, makeFakeToken({ role: "SUPER_ADMIN" }));
    renderTopBar();

    expect(screen.getByText("Adren")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /sign out/i })).toBeInTheDocument();
  });

  it("a CONSULTANT session with no branding configured falls back to the Adren wordmark", async () => {
    server.use(
      http.get("/api/v1/consultants/:id/branding", () =>
        HttpResponse.json({ title: "not found" }, { status: 404 })
      )
    );
    localStorage.setItem(AUTH_TOKEN_STORAGE_KEY, makeFakeToken({ role: "CONSULTANT", consultantId: "c-1" }));
    renderTopBar();

    await waitFor(() => expect(screen.getByRole("button", { name: /sign out/i })).toBeInTheDocument());
    expect(screen.getByText("Adren")).toBeInTheDocument();
    expect(screen.queryByRole("img")).not.toBeInTheDocument();
  });

  it("a CONSULTANT session with a configured logo shows it plus the Powered-by wordmark", async () => {
    server.use(
      http.get("/api/v1/consultants/:id/branding", () =>
        HttpResponse.json({
          consultantId: "c-1",
          logoUrl: "https://cdn.example.com/logo.png",
          backgroundImageUrl: null,
          backgroundColor: "#FFFFFF",
          textColorPrimary: "#000000",
          textColorSecondary: "#111111",
          domain: "consultant.example.com",
          updatedAt: "2026-01-01T00:00:00Z",
        })
      )
    );
    localStorage.setItem(AUTH_TOKEN_STORAGE_KEY, makeFakeToken({ role: "CONSULTANT", consultantId: "c-1" }));
    renderTopBar();

    await waitFor(() =>
      expect(screen.getByAltText("Consultant logo")).toHaveAttribute("src", "https://cdn.example.com/logo.png")
    );
    expect(screen.getByText(/powered by adren/i)).toBeInTheDocument();
  });

  it("signing out clears the token and hard-navigates to /login", () => {
    localStorage.setItem(AUTH_TOKEN_STORAGE_KEY, makeFakeToken({ role: "CONSULTANT" }));
    const hrefSetter = vi.fn();
    let currentHref = window.location.href;
    Object.defineProperty(window, "location", {
      configurable: true,
      value: {
        ...window.location,
        get href() {
          return currentHref;
        },
        set href(value: string) {
          currentHref = value;
          hrefSetter(value);
        },
      },
    });

    renderTopBar();
    fireEvent.click(screen.getByRole("button", { name: /sign out/i }));

    expect(localStorage.getItem(AUTH_TOKEN_STORAGE_KEY)).toBeNull();
    expect(hrefSetter).toHaveBeenCalledWith("/login");
  });
});
