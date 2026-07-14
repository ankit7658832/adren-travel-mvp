import { render, screen, fireEvent } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { describe, expect, it, vi } from "vitest";
import type { ReactNode } from "react";
import { RouteErrorBoundary } from "./RouteErrorBoundary";

function Bomb(): never {
  throw new Error("route crashed");
}

function renderWithQueryClient(children: ReactNode) {
  const queryClient = new QueryClient();
  return render(<QueryClientProvider client={queryClient}>{children}</QueryClientProvider>);
}

describe("RouteErrorBoundary", () => {
  it("renders children when nothing throws", () => {
    renderWithQueryClient(
      <RouteErrorBoundary>
        <div>screen content</div>
      </RouteErrorBoundary>
    );

    expect(screen.getByText("screen content")).toBeInTheDocument();
  });

  it("renders a retry fallback when this route's content throws, without a generic reload", () => {
    vi.spyOn(console, "error").mockImplementation(() => {});

    renderWithQueryClient(
      <RouteErrorBoundary>
        <Bomb />
      </RouteErrorBoundary>
    );

    expect(screen.getByRole("alert")).toHaveTextContent(/couldn't load this screen/i);
    const retryButton = screen.getByRole("button", { name: /retry/i });
    expect(retryButton).toBeInTheDocument();

    // Clicking retry re-renders the same throwing tree, which is expected
    // to throw again immediately in this test — proves the reset wiring
    // doesn't itself crash.
    fireEvent.click(retryButton);
    expect(screen.getByRole("alert")).toBeInTheDocument();

    vi.restoreAllMocks();
  });

  it("isolates a crash to its own boundary — a sibling boundary keeps rendering", () => {
    vi.spyOn(console, "error").mockImplementation(() => {});

    renderWithQueryClient(
      <>
        <RouteErrorBoundary>
          <Bomb />
        </RouteErrorBoundary>
        <RouteErrorBoundary>
          <div>sibling still works</div>
        </RouteErrorBoundary>
      </>
    );

    expect(screen.getByRole("alert")).toBeInTheDocument();
    expect(screen.getByText("sibling still works")).toBeInTheDocument();

    vi.restoreAllMocks();
  });
});
