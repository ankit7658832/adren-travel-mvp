import { render, screen, within } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { CampaignStatusStepper } from "./CampaignStatusStepper";

/** ADS-08's own AC: one case per AdCampaignStatus enum value, asserting the stepper highlights the exact matching stage. */
describe("CampaignStatusStepper", () => {
  it("PENDING_APPROVAL: highlights the first step", () => {
    render(<CampaignStatusStepper status="PENDING_APPROVAL" />);

    const stepper = screen.getByLabelText("campaign-status-stepper");
    expect(within(stepper).getByText("Pending Approval")).toHaveAttribute("aria-current", "step");
  });

  it("PENDING_POLICY_REVIEW: highlights the second step, the first shows as completed", () => {
    render(<CampaignStatusStepper status="PENDING_POLICY_REVIEW" />);

    const stepper = screen.getByLabelText("campaign-status-stepper");
    expect(within(stepper).getByText("Pending Policy Review")).toHaveAttribute("aria-current", "step");
    expect(within(stepper).getByText("Pending Approval")).not.toHaveAttribute("aria-current");
  });

  it("LIVE: highlights the Live step", () => {
    render(<CampaignStatusStepper status="LIVE" />);

    const stepper = screen.getByLabelText("campaign-status-stepper");
    expect(within(stepper).getByText("Live")).toHaveAttribute("aria-current", "step");
    expect(within(stepper).queryByText("Paused")).not.toBeInTheDocument();
    expect(within(stepper).queryByText("Spend Cap Reached")).not.toBeInTheDocument();
  });

  it("PAUSED: highlights Live with a Paused badge", () => {
    render(<CampaignStatusStepper status="PAUSED" />);

    const stepper = screen.getByLabelText("campaign-status-stepper");
    expect(within(stepper).getByText("Live")).toHaveAttribute("aria-current", "step");
    expect(within(stepper).getByText("Paused")).toBeInTheDocument();
  });

  it("SPEND_CAP_REACHED: highlights Live with a Spend Cap Reached badge", () => {
    render(<CampaignStatusStepper status="SPEND_CAP_REACHED" />);

    const stepper = screen.getByLabelText("campaign-status-stepper");
    expect(within(stepper).getByText("Live")).toHaveAttribute("aria-current", "step");
    expect(within(stepper).getByText("Spend Cap Reached")).toBeInTheDocument();
  });

  it("REJECTED: shows the Rejected badge instead of the Live step", () => {
    render(<CampaignStatusStepper status="REJECTED" />);

    const stepper = screen.getByLabelText("campaign-status-stepper");
    expect(within(stepper).getByText("Rejected")).toBeInTheDocument();
    expect(within(stepper).queryByText("Live")).not.toBeInTheDocument();
  });
});
