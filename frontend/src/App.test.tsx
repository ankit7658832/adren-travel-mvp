import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { describe, expect, it } from "vitest";
import App from "./App";

// SearchDashboard (mounted at "/") now drives a real React Query mutation
// (FND-13), so it needs a QueryClientProvider ancestor even in a routing
// test that never triggers a search.
function renderApp(initialEntry: string) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={[initialEntry]}>
        <App />
      </MemoryRouter>
    </QueryClientProvider>
  );
}

/**
 * FES-01: every PRD Part 21 screen is a React.lazy chunk behind a route —
 * this asserts the Suspense boundary resolves the correct lazy chunk per
 * route rather than just checking the route table compiles.
 */
describe("App routing (FES-01 code-split routes)", () => {
  it("resolves the default route's lazy chunk", async () => {
    renderApp("/");

    expect(await screen.findByText("Search & Build Itinerary")).toBeInTheDocument();
  });

  it("resolves a not-yet-built screen's lazy chunk with its placeholder", async () => {
    renderApp("/admin");

    expect(await screen.findByText("21.6 Super Admin Console")).toBeInTheDocument();
  });

  it("resolves the storefront route's lazy chunk", async () => {
    renderApp("/storefront");

    expect(await screen.findByRole("heading", { level: 1 })).toBeInTheDocument();
  });
});
