import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { RootErrorBoundary } from "./RootErrorBoundary";

function Bomb(): never {
  throw new Error("boom");
}

describe("RootErrorBoundary", () => {
  it("renders children when nothing throws", () => {
    render(
      <RootErrorBoundary>
        <div>all good</div>
      </RootErrorBoundary>
    );

    expect(screen.getByText("all good")).toBeInTheDocument();
  });

  it("renders a generic reload fallback when a descendant throws", () => {
    // React logs the caught error to the console by default — silence it
    // for this expected-throw test.
    vi.spyOn(console, "error").mockImplementation(() => {});

    render(
      <RootErrorBoundary>
        <Bomb />
      </RootErrorBoundary>
    );

    expect(screen.getByRole("alert")).toHaveTextContent(/something went wrong/i);
    expect(screen.getByRole("button", { name: /reload/i })).toBeInTheDocument();

    vi.restoreAllMocks();
  });
});
