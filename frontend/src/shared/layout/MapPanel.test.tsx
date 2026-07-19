import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import { MapPanel, type MapPin } from "./MapPanel";

describe("MapPanel", () => {
  it("renders nothing for an empty pin list", () => {
    const { container } = render(<MapPanel pins={[]} ariaLabel="Map" />);
    expect(container).toBeEmptyDOMElement();
  });

  it("renders one pin per location, including one with no inventory (T1)", () => {
    const pins: MapPin[] = [
      { id: "GOA", latitude: 15.5, longitude: 73.8, label: "Goa", hasInventory: true },
      { id: "ANT", latitude: 10, longitude: 90, label: "Antarctica", hasInventory: false },
    ];

    render(<MapPanel pins={pins} ariaLabel="Map showing 2 searched locations" />);

    const rendered = screen.getAllByTestId("map-pin");
    expect(rendered).toHaveLength(2);
    expect(rendered[0]).toHaveAttribute("data-has-inventory", "true");
    expect(rendered[1]).toHaveAttribute("data-has-inventory", "false");
    expect(screen.getByText(/antarctica \(no inventory\)/i)).toBeInTheDocument();
  });

  it("treats an omitted hasInventory as inventory-present (Itinerary Builder's own line-item pins)", () => {
    render(
      <MapPanel
        pins={[{ id: "GOA", latitude: 15, longitude: 74, label: "Goa" }]}
        ariaLabel="Map showing 1 itinerary location"
      />
    );

    const pin = screen.getByTestId("map-pin");
    expect(pin).not.toHaveAttribute("data-has-inventory");
    expect(screen.queryByText(/no inventory/i)).not.toBeInTheDocument();
  });

  it("exposes the caller-supplied accessible name via role=img", () => {
    render(
      <MapPanel
        pins={[{ id: "GOA", latitude: 15, longitude: 74, label: "Goa" }]}
        ariaLabel="Map showing 1 searched location"
      />
    );

    expect(screen.getByRole("img", { name: "Map showing 1 searched location" })).toBeInTheDocument();
  });
});
