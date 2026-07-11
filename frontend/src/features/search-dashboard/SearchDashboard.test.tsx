import { describe, expect, it } from "vitest";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { SearchDashboard } from "./SearchDashboard";

/**
 * Component-level integration test — renders the real component tree
 * (hook + component together), asserting on user-visible behavior rather
 * than implementation details. Traces to PRD Section 22.1 acceptance
 * criteria ("multi-location search... every location, even if one has no
 * inventory").
 */
describe("SearchDashboard", () => {
  it("shows a loading state immediately after search is submitted", async () => {
    render(<SearchDashboard />);

    fireEvent.change(screen.getByLabelText(/locations/i), {
      target: { value: "Goa, Udaipur" },
    });
    fireEvent.click(screen.getByRole("button", { name: /search/i }));

    expect(screen.getByRole("status")).toHaveTextContent(/loading/i);
  });

  it("renders one result item per submitted location", async () => {
    render(<SearchDashboard />);

    fireEvent.change(screen.getByLabelText(/locations/i), {
      target: { value: "Goa, Udaipur, Jaipur" },
    });
    fireEvent.click(screen.getByRole("button", { name: /search/i }));

    await waitFor(() => {
      expect(screen.getByLabelText("search-results").children).toHaveLength(3);
    });
  });

  it("ignores blank/whitespace-only location entries", async () => {
    render(<SearchDashboard />);

    fireEvent.change(screen.getByLabelText(/locations/i), {
      target: { value: "Goa, , Udaipur" },
    });
    fireEvent.click(screen.getByRole("button", { name: /search/i }));

    await waitFor(() => {
      expect(screen.getByLabelText("search-results").children).toHaveLength(2);
    });
  });
});
