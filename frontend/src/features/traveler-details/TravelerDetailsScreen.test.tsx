import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { describe, expect, it } from "vitest";
import { http, HttpResponse } from "msw";
import { TravelerDetailsScreen } from "./TravelerDetailsScreen";
import { server } from "@/test/mswServer";

function renderScreen() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <TravelerDetailsScreen />
    </QueryClientProvider>
  );
}

describe("TravelerDetailsScreen (SCR-14)", () => {
  it("default state: shows one empty traveler section, submit disabled until required fields are filled", () => {
    renderScreen();

    expect(screen.getByRole("heading", { name: "Traveler 1" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /save traveler details/i })).toBeDisabled();
    expect(screen.queryByLabelText(/passport number/i)).not.toBeInTheDocument();
  });

  it("shows passport fields only when the international/cruise toggle is checked", () => {
    renderScreen();

    fireEvent.click(screen.getByLabelText(/international travel or a cruise/i));

    expect(screen.getByLabelText(/passport number/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/passport expiry/i)).toBeInTheDocument();
  });

  it("adds another traveler section via + Add Traveler", () => {
    renderScreen();

    fireEvent.click(screen.getByRole("button", { name: /add traveler/i }));

    expect(screen.getByRole("heading", { name: "Traveler 1" })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Traveler 2" })).toBeInTheDocument();
  });

  it("enables submit once required fields are filled, and shows a completion checkmark", () => {
    renderScreen();

    expect(screen.queryByLabelText("complete")).not.toBeInTheDocument();

    fireEvent.change(screen.getByLabelText(/full name/i), { target: { value: "Jane Traveler" } });
    fireEvent.change(screen.getByLabelText(/date of birth/i), { target: { value: "1990-05-01" } });

    expect(screen.getByLabelText("complete")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /save traveler details/i })).toBeEnabled();
  });

  it("submits each traveler and shows a success confirmation", async () => {
    let requestCount = 0;
    server.use(
      http.post("/api/v1/travelers", () => {
        requestCount += 1;
        return HttpResponse.json({ travelerId: `trav-${requestCount}` }, { status: 201 });
      })
    );
    renderScreen();

    fireEvent.change(screen.getByLabelText(/full name/i), { target: { value: "Jane Traveler" } });
    fireEvent.change(screen.getByLabelText(/date of birth/i), { target: { value: "1990-05-01" } });
    fireEvent.click(screen.getByRole("button", { name: /save traveler details/i }));

    await waitFor(() => {
      expect(screen.getByRole("status")).toHaveTextContent(/saved 1 traveler/i);
    });
    expect(requestCount).toBe(1);
  });

  it("shows an inline error when saving fails", async () => {
    server.use(http.post("/api/v1/travelers", () => HttpResponse.json({ title: "Server error" }, { status: 500 })));
    renderScreen();

    fireEvent.change(screen.getByLabelText(/full name/i), { target: { value: "Jane Traveler" } });
    fireEvent.change(screen.getByLabelText(/date of birth/i), { target: { value: "1990-05-01" } });
    fireEvent.click(screen.getByRole("button", { name: /save traveler details/i }));

    await waitFor(() => {
      expect(screen.getByRole("alert")).toHaveTextContent(/could not save traveler details/i);
    });
  });
});
