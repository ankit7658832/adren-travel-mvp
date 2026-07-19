import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { Button } from "./Button";

describe("Button", () => {
  it("renders its label and responds to clicks", async () => {
    const onClick = vi.fn();
    render(<Button onClick={onClick}>Confirm booking</Button>);

    await userEvent.click(screen.getByRole("button", { name: "Confirm booking" }));

    expect(onClick).toHaveBeenCalledTimes(1);
  });

  it("does not fire onClick when disabled", async () => {
    const onClick = vi.fn();
    render(
      <Button onClick={onClick} disabled>
        Confirm booking
      </Button>
    );

    await userEvent.click(screen.getByRole("button", { name: "Confirm booking" }));

    expect(onClick).not.toHaveBeenCalled();
  });

  it("applies the destructive variant's classes", () => {
    render(<Button variant="destructive">Cancel booking</Button>);

    expect(screen.getByRole("button", { name: "Cancel booking" })).toHaveClass("bg-error-600");
  });
});
