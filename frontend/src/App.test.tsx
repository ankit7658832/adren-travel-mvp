import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it } from "vitest";
import App from "./App";

/**
 * FES-01: every PRD Part 21 screen is a React.lazy chunk behind a route —
 * this asserts the Suspense boundary resolves the correct lazy chunk per
 * route rather than just checking the route table compiles.
 */
describe("App routing (FES-01 code-split routes)", () => {
  it("resolves the default route's lazy chunk", async () => {
    render(
      <MemoryRouter initialEntries={["/"]}>
        <App />
      </MemoryRouter>
    );

    expect(await screen.findByText("Search & Build Itinerary")).toBeInTheDocument();
  });

  it("resolves a not-yet-built screen's lazy chunk with its placeholder", async () => {
    render(
      <MemoryRouter initialEntries={["/admin"]}>
        <App />
      </MemoryRouter>
    );

    expect(await screen.findByText("21.6 Super Admin Console")).toBeInTheDocument();
  });

  it("resolves the storefront route's lazy chunk", async () => {
    render(
      <MemoryRouter initialEntries={["/storefront"]}>
        <App />
      </MemoryRouter>
    );

    expect(await screen.findByRole("heading", { level: 1 })).toBeInTheDocument();
  });
});
