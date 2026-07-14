import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it } from "vitest";
import { ConsultantStorefront } from "./ConsultantStorefront";

/**
 * doc/DESIGN.md §3.3, §12 item 4 — verifies the contrast-safety algorithm
 * actually drives this screen end-to-end: a safe preset needs no
 * intervention, an unsafe preset triggers the fallback chain and blocks
 * the save button, matching the documented behavior.
 */
function renderStorefront() {
  return render(<ConsultantStorefront />);
}

describe("ConsultantStorefront", () => {
  it("renders the safe preset by default with save enabled", async () => {
    renderStorefront();

    expect(
      (await screen.findAllByText(/Nordic Fjord Journeys/i)).length
    ).toBeGreaterThan(0);
    expect(screen.getByRole("button", { name: /save branding/i })).toBeEnabled();
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  });

  it("blocks save and shows the fallback warning for the fallback-required preset", async () => {
    const user = userEvent.setup();
    renderStorefront();

    await user.click(
      screen.getByText(/Coastal Drift Co — fallback-required pick/i)
    );

    expect(
      await screen.findByRole("button", { name: /save branding/i })
    ).toBeDisabled();
    expect(screen.getByRole("alert")).toHaveTextContent(/save is blocked/i);
  });

  it("shows a fixed, non-tenant-colored price on the quotation card regardless of preset", async () => {
    const user = userEvent.setup();
    renderStorefront();

    await user.click(
      screen.getByText(/Coastal Drift Co — fallback-required pick/i)
    );

    const priceCard = screen.getByText(/₹1,24,500/i);
    expect(priceCard).toBeInTheDocument();
    expect(within(priceCard.closest("div")!).getByText(/Confirmed/i)).toBeInTheDocument();
  });
});
