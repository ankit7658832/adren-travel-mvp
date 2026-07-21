import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { describe, expect, it, afterEach } from "vitest";
import App from "./App";
import { AuthSessionProvider } from "./shared/auth/AuthSessionContext";
import { AUTH_TOKEN_STORAGE_KEY, type Role } from "./shared/auth/authTypes";
import { makeFakeToken } from "./shared/auth/testAuthTokens";

// SearchDashboard (mounted at "/") now drives a real React Query mutation
// (FND-13), so it needs a QueryClientProvider ancestor even in a routing
// test that never triggers a search. FES-07: most routes are now guarded
// (App.tsx), so renderApp optionally seeds a session's role first — the
// same AuthSessionProvider slot main.tsx mounts via AppProviders.
function renderApp(initialEntry: string, role?: Role) {
  if (role) {
    localStorage.setItem(AUTH_TOKEN_STORAGE_KEY, makeFakeToken({ role }));
  }
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <AuthSessionProvider>
        <MemoryRouter initialEntries={[initialEntry]}>
          <App />
        </MemoryRouter>
      </AuthSessionProvider>
    </QueryClientProvider>
  );
}

/**
 * FES-01: every PRD Part 21 screen is a React.lazy chunk behind a route —
 * this asserts the Suspense boundary resolves the correct lazy chunk per
 * route rather than just checking the route table compiles.
 */
describe("App routing (FES-01 code-split routes)", () => {
  afterEach(() => {
    localStorage.clear();
  });

  it("resolves the default route's lazy chunk", async () => {
    renderApp("/");

    expect(await screen.findByText("Search & Build Itinerary")).toBeInTheDocument();
  });

  it("resolves the Super Admin Console's lazy chunk for an authorized SUPER_ADMIN session", async () => {
    renderApp("/admin", "SUPER_ADMIN");

    expect(await screen.findByText(/21\.6 Super Admin Console/i)).toBeInTheDocument();
  });

  it("FES-07: a USER-role session hitting the Super Admin Console route is redirected to / before it mounts", async () => {
    renderApp("/admin", "USER");

    expect(await screen.findByText("Search & Build Itinerary")).toBeInTheDocument();
    expect(screen.queryByText(/21\.6 Super Admin Console/i)).not.toBeInTheDocument();
  });

  it("resolves the storefront route's lazy chunk with no session at all, since it's Layer 2/unguarded", async () => {
    renderApp("/storefront");

    expect(await screen.findByRole("heading", { level: 1 })).toBeInTheDocument();
  });
});
