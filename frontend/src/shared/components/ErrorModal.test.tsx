import { render, screen, fireEvent } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { ErrorModal } from "./ErrorModal";

describe("ErrorModal (SCR-16)", () => {
  it("renders the title, message, and a Retry button when onRetry is given", () => {
    const onRetry = vi.fn();
    render(<ErrorModal title="Search failed" message="Could not reach the server." onRetry={onRetry} onDismiss={vi.fn()} />);

    expect(screen.getByRole("dialog")).toBeInTheDocument();
    expect(screen.getByText("Search failed")).toBeInTheDocument();
    expect(screen.getByRole("alert")).toHaveTextContent("Could not reach the server.");

    fireEvent.click(screen.getByRole("button", { name: /retry/i }));
    expect(onRetry).toHaveBeenCalled();
  });

  it("omits the Retry button when onRetry isn't given", () => {
    render(<ErrorModal message="No supplier availability." onDismiss={vi.fn()} />);

    expect(screen.queryByRole("button", { name: /retry/i })).not.toBeInTheDocument();
  });

  it("calls onDismiss when the Dismiss button is clicked", () => {
    const onDismiss = vi.fn();
    render(<ErrorModal message="Failed." onDismiss={onDismiss} />);

    fireEvent.click(screen.getByRole("button", { name: /dismiss/i }));
    expect(onDismiss).toHaveBeenCalled();
  });

  it("calls onDismiss when the Escape key is pressed", () => {
    const onDismiss = vi.fn();
    render(<ErrorModal message="Failed." onDismiss={onDismiss} />);

    fireEvent.keyDown(window, { key: "Escape" });
    expect(onDismiss).toHaveBeenCalled();
  });

  it("calls onDismiss when the scrim is clicked, but not when the dialog content is clicked", () => {
    const onDismiss = vi.fn();
    render(<ErrorModal message="Failed." onDismiss={onDismiss} />);

    fireEvent.click(screen.getByRole("dialog"));
    expect(onDismiss).not.toHaveBeenCalled();

    fireEvent.click(screen.getByRole("button", { name: /close error dialog/i }));
    expect(onDismiss).toHaveBeenCalled();
  });
});
