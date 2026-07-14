import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import { MapPanel } from "./MapPanel";
import type { LocationResult } from "./useMultiLocationSearch";

describe("MapPanel", () => {
  it("renders nothing for an empty location list", () => {
    const { container } = render(<MapPanel locations={[]} />);
    expect(container).toBeEmptyDOMElement();
  });

  it("renders one pin per location, including one with no inventory (T1)", () => {
    const locations: LocationResult[] = [
      { locationCode: "GOA", displayName: "Goa", latitude: 15.5, longitude: 73.8, hasInventory: true, autoSelectedSupplierId: "HOTELBEDS", autoSelectedSupplierRateId: "rate-1" },
      { locationCode: "ANT", displayName: "Antarctica", latitude: 10, longitude: 90, hasInventory: false, autoSelectedSupplierId: null, autoSelectedSupplierRateId: null },
    ];

    render(<MapPanel locations={locations} />);

    const pins = screen.getAllByTestId("map-pin");
    expect(pins).toHaveLength(2);
    expect(pins[0]).toHaveAttribute("data-has-inventory", "true");
    expect(pins[1]).toHaveAttribute("data-has-inventory", "false");
    expect(screen.getByText(/antarctica \(no inventory\)/i)).toBeInTheDocument();
  });

  it("exposes an accessible name via role=img for screen readers", () => {
    render(<MapPanel locations={[{ locationCode: "GOA", displayName: "Goa", latitude: 15, longitude: 74, hasInventory: true, autoSelectedSupplierId: "HOTELBEDS", autoSelectedSupplierRateId: "rate-1" }]} />);

    expect(screen.getByRole("img", { name: /map showing 1 searched location/i })).toBeInTheDocument();
  });
});
