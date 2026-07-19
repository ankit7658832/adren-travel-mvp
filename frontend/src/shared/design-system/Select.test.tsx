import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { Select } from "./Select";

const marketOptions = [
  { value: "INDIA", label: "India" },
  { value: "UK", label: "United Kingdom" },
];

describe("Select", () => {
  it("links the label to the select and renders every option", () => {
    render(<Select label="Home market" options={marketOptions} />);

    const select = screen.getByLabelText("Home market");
    expect(select).not.toHaveAttribute("aria-invalid");
    expect(screen.getByRole("option", { name: "India" })).toBeInTheDocument();
    expect(screen.getByRole("option", { name: "United Kingdom" })).toBeInTheDocument();
  });

  it("wires aria-invalid and aria-describedby to the error message, same contract as TextField", () => {
    render(<Select label="Home market" options={marketOptions} error="Select a market" />);

    const select = screen.getByLabelText("Home market");
    const error = screen.getByRole("alert");

    expect(select).toHaveAttribute("aria-invalid", "true");
    expect(select).toHaveAttribute("aria-describedby", error.id);
    expect(error).toHaveTextContent("Select a market");
  });
});
