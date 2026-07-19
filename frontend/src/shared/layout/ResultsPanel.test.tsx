import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import { ResultsPanel } from "./ResultsPanel";

describe("ResultsPanel", () => {
  it("renders the map before the results list, sharing one split-panel structure (FES-05 AC)", () => {
    render(
      <ResultsPanel map={<div data-testid="map-slot">map content</div>} ariaLabel="results">
        <li key="1">Result one</li>
        <li key="2">Result two</li>
      </ResultsPanel>
    );

    const list = screen.getByLabelText("results");
    expect(list.tagName).toBe("UL");
    expect(list.children).toHaveLength(2);
    expect(screen.getByTestId("map-slot")).toBeInTheDocument();

    // Map slot precedes the results list in source order — mobile stacks map on top,
    // desktop's `md:flex-row` puts the same first-child on the left, per §21.1/§21.2.
    const container = list.parentElement;
    expect(container?.children[0]).toContainElement(screen.getByTestId("map-slot"));
    expect(container?.children[1]).toBe(list);
  });
});
