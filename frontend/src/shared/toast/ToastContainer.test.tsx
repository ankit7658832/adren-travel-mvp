import { act, render, screen } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { ToastContainer } from "./ToastContainer";
import { useToastQueueStore } from "./toastQueueStore";

describe("ToastContainer (FES-10)", () => {
  beforeEach(() => {
    vi.useFakeTimers();
    useToastQueueStore.setState({ toasts: [] });
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it("FES-10 AC: a queued success toast renders via a status live region", () => {
    render(<ToastContainer />);

    act(() => {
      useToastQueueStore.getState().addToast({ message: "Branding saved.", tone: "success" });
    });

    expect(screen.getByRole("status")).toHaveTextContent("Branding saved.");
  });

  it("FES-10 AC: a queued error toast renders via an alert live region", () => {
    render(<ToastContainer />);

    act(() => {
      useToastQueueStore.getState().addToast({ message: "Payment failed.", tone: "error" });
    });

    expect(screen.getByRole("alert")).toHaveTextContent("Payment failed.");
  });

  it("auto-dismisses a queued toast after the timeout", () => {
    render(<ToastContainer />);

    act(() => {
      useToastQueueStore.getState().addToast({ message: "Package published.", tone: "success" });
    });
    expect(screen.getByText("Package published.")).toBeInTheDocument();

    act(() => {
      vi.advanceTimersByTime(5000);
    });

    expect(screen.queryByText("Package published.")).not.toBeInTheDocument();
  });

  it("renders multiple queued toasts independently, each with its own dismiss timer", () => {
    render(<ToastContainer />);

    act(() => {
      useToastQueueStore.getState().addToast({ message: "First saved.", tone: "success" });
    });
    act(() => {
      vi.advanceTimersByTime(2000);
    });
    act(() => {
      useToastQueueStore.getState().addToast({ message: "Second saved.", tone: "success" });
    });

    expect(screen.getByText("First saved.")).toBeInTheDocument();
    expect(screen.getByText("Second saved.")).toBeInTheDocument();

    // First toast's 5s timer elapses (2s + 3s); second's hasn't (only 3s in).
    act(() => {
      vi.advanceTimersByTime(3000);
    });
    expect(screen.queryByText("First saved.")).not.toBeInTheDocument();
    expect(screen.getByText("Second saved.")).toBeInTheDocument();
  });
});
