import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { Card } from "./Card";

describe("Card", () => {
  it("renders its children inside the fixed surface/border/radius shell", () => {
    render(<Card data-testid="card">Hotel result content</Card>);

    const card = screen.getByTestId("card");
    expect(card).toHaveTextContent("Hotel result content");
    expect(card).toHaveClass("bg-surface", "border-neutral-200", "rounded-lg");
  });

  it("defaults to md (space-6) padding and switches to sm (space-4) when requested", () => {
    const { rerender } = render(<Card data-testid="card">content</Card>);
    expect(screen.getByTestId("card")).toHaveClass("p-6");

    rerender(
      <Card data-testid="card" padding="sm">
        content
      </Card>
    );
    expect(screen.getByTestId("card")).toHaveClass("p-4");
  });
});
