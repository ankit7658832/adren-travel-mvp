import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { TextField } from "./TextField";

describe("TextField", () => {
  it("links the label to the input via htmlFor/id, with no error state by default", () => {
    render(<TextField label="Traveler full name" />);

    const input = screen.getByLabelText("Traveler full name");
    expect(input).not.toHaveAttribute("aria-invalid");
    expect(input).not.toHaveAttribute("aria-describedby");
  });

  it("wires aria-invalid and aria-describedby to the error message automatically (FES-04 AC)", () => {
    render(<TextField label="Passport number" error="Passport number is required" />);

    const input = screen.getByLabelText("Passport number");
    const error = screen.getByRole("alert");

    expect(input).toHaveAttribute("aria-invalid", "true");
    expect(input).toHaveAttribute("aria-describedby", error.id);
    expect(error).toHaveTextContent("Passport number is required");
  });
});
