import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { AppProviders } from "./AppProviders";

/**
 * FES-02: AppProviders is the documented slot for app-wide context
 * providers between QueryClientProvider and BrowserRouter in main.tsx.
 * It's currently empty (see AppProviders.tsx's doc comment — theme/
 * branding state is a Zustand store, not a Context, per
 * doc/architecture/RULES.md §7.1, so it doesn't occupy this slot), so the
 * only invariant to assert today is that the slot passes children through
 * unmodified. Once a real provider (most likely auth, FES-06/FES-07) lands
 * here, extend this test to assert that provider's context is reachable by
 * descendants, the same way a context-based provider's contract should be
 * tested.
 */
describe("AppProviders (FES-02 provider-stack slot)", () => {
  it("renders its children unmodified", () => {
    render(
      <AppProviders>
        <div data-testid="child">child content</div>
      </AppProviders>
    );

    expect(screen.getByTestId("child")).toHaveTextContent("child content");
  });
});
