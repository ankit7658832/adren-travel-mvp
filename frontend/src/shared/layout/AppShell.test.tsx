import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { describe, expect, it } from "vitest";
import { AppShell } from "./AppShell";
import { AuthSessionProvider } from "@/shared/auth/AuthSessionContext";

describe("AppShell (doc/ADREN_UIUX_SPEC.md §3)", () => {
  it("renders the nav shell around its children without introducing a second <main> landmark", () => {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    render(
      <QueryClientProvider client={queryClient}>
        <AuthSessionProvider>
          <MemoryRouter>
            <AppShell>
              <main>
                <h1>Screen content</h1>
              </main>
            </AppShell>
          </MemoryRouter>
        </AuthSessionProvider>
      </QueryClientProvider>
    );

    expect(screen.getByRole("heading", { name: "Screen content" })).toBeInTheDocument();
    expect(screen.getAllByRole("main")).toHaveLength(1);
    expect(screen.getByRole("navigation", { name: "main navigation" })).toBeInTheDocument();
    expect(screen.getByRole("navigation", { name: "mobile navigation" })).toBeInTheDocument();
  });
});
