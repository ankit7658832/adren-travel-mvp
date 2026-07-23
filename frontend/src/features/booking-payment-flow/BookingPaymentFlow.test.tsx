import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter, Route, Routes, useParams } from "react-router-dom";
import { describe, expect, it } from "vitest";
import { http, HttpResponse } from "msw";
import { BookingPaymentFlow } from "./BookingPaymentFlow";
import { server } from "@/test/mswServer";

const PACKAGE = {
  packageId: "pkg-1",
  sourceItineraryId: "itin-1",
  consultantId: "c-1",
  name: "Goa Beach Escape",
  description: "3 nights in Goa",
  validityStart: "2026-01-01",
  validityEnd: "2026-12-31",
  basePrice: "10000.00",
  markupPrice: "1500.00",
  currency: "INR",
  maxPax: 4,
  promotedViaAds: false,
};

function mockWallet(overrides: Partial<Record<string, string>> = {}) {
  server.use(
    http.get("/api/v1/wallet", () =>
      HttpResponse.json({
        consultantId: "c-1",
        availableBalance: "50000.00",
        creditLimit: "0.00",
        pendingHolds: "0.00",
        currency: "INR",
        updatedAt: "2026-01-01T00:00:00Z",
        ...overrides,
      })
    )
  );
}

function ConfirmationRouteStub() {
  const { bookingId } = useParams<{ bookingId: string }>();
  return <p role="status">Booking confirmed — id {bookingId}</p>;
}

function renderFlow() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={["/booking/pkg-1"]}>
        <Routes>
          <Route path="/booking/:packageId" element={<BookingPaymentFlow />} />
          {/* Stands in for the real BookingConfirmation screen (its own
              component test), just proving SCR-18 navigates there on
              success with the right booking id. */}
          <Route path="/bookings/:bookingId/confirmation" element={<ConfirmationRouteStub />} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>
  );
}

describe("BookingPaymentFlow (HRD-15, completing BOK-13)", () => {
  it("loading state: shows a loading message while fetching the package", () => {
    server.use(http.get("/api/v1/packages/pkg-1", () => new Promise<Response>(() => {})));
    renderFlow();

    expect(screen.getByRole("status")).toHaveTextContent(/loading package details/i);
  });

  it("error state: shows a retry option when the package fetch fails", async () => {
    server.use(http.get("/api/v1/packages/pkg-1", () => HttpResponse.json({ title: "not found" }, { status: 404 })));
    renderFlow();

    await waitFor(() => {
      expect(screen.getByRole("alert")).toHaveTextContent(/could not load this package/i);
    });
    expect(screen.getByRole("button", { name: /retry/i })).toBeInTheDocument();
  });

  it("shows the package name/description and a net+markup price breakdown before payment method selection", async () => {
    server.use(http.get("/api/v1/packages/pkg-1", () => HttpResponse.json(PACKAGE)));
    mockWallet();
    renderFlow();

    await waitFor(() => {
      expect(screen.getByRole("heading", { name: "Goa Beach Escape" })).toBeInTheDocument();
    });
    expect(screen.getByText(/3 nights in goa/i)).toBeInTheDocument();
    expect(screen.getByText(/net \(supplier cost\)/i)).toBeInTheDocument();
    expect(screen.getByText("Total: INR 11500.00")).toBeInTheDocument();
    expect(screen.getByLabelText(/^wallet$/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/on-account/i)).toBeInTheDocument();
  });

  it("submits traveler details then confirms the booking via the wallet path, showing the new booking id", async () => {
    server.use(http.get("/api/v1/packages/pkg-1", () => HttpResponse.json(PACKAGE)));
    mockWallet();
    let travelerBody: unknown;
    let bookingBody: unknown;
    server.use(
      http.post("/api/v1/travelers", async ({ request }) => {
        travelerBody = await request.json();
        return HttpResponse.json({ travelerId: "trav-1" }, { status: 201 });
      })
    );
    server.use(
      http.post("/api/v1/bookings", async ({ request }) => {
        bookingBody = await request.json();
        return HttpResponse.json({ bookingId: "booking-1" });
      })
    );
    renderFlow();

    await waitFor(() => expect(screen.getByRole("heading", { name: "Goa Beach Escape" })).toBeInTheDocument());

    fireEvent.change(screen.getByLabelText(/full name/i), { target: { value: "Jane Traveler" } });
    fireEvent.change(screen.getByLabelText(/date of birth/i), { target: { value: "1990-05-01" } });
    fireEvent.click(screen.getByRole("button", { name: /confirm booking/i }));

    await waitFor(() => {
      expect(screen.getByRole("status")).toHaveTextContent(/booking confirmed — id booking-1/i);
    });
    expect(travelerBody).toMatchObject({ name: "Jane Traveler", dateOfBirth: "1990-05-01" });
    expect(bookingBody).toEqual({ quotationOrPackageId: "pkg-1", totalSellPrice: "11500.00", currency: "INR" });
  });

  it("confirms via the On-Account path when that payment method is selected", async () => {
    server.use(http.get("/api/v1/packages/pkg-1", () => HttpResponse.json(PACKAGE)));
    mockWallet();
    server.use(http.post("/api/v1/travelers", () => HttpResponse.json({ travelerId: "trav-1" }, { status: 201 })));
    let onAccountCalled = false;
    server.use(
      http.post("/api/v1/bookings/on-account", () => {
        onAccountCalled = true;
        return HttpResponse.json({ bookingId: "booking-2" });
      })
    );
    renderFlow();

    await waitFor(() => expect(screen.getByRole("heading", { name: "Goa Beach Escape" })).toBeInTheDocument());

    fireEvent.change(screen.getByLabelText(/full name/i), { target: { value: "Jane Traveler" } });
    fireEvent.change(screen.getByLabelText(/date of birth/i), { target: { value: "1990-05-01" } });
    fireEvent.click(screen.getByLabelText(/on-account/i));
    fireEvent.click(screen.getByRole("button", { name: /confirm booking/i }));

    await waitFor(() => {
      expect(screen.getByRole("status")).toHaveTextContent(/booking confirmed — id booking-2/i);
    });
    expect(onAccountCalled).toBe(true);
  });

  it("shows an inline error and does not crash when confirming the booking fails", async () => {
    server.use(http.get("/api/v1/packages/pkg-1", () => HttpResponse.json(PACKAGE)));
    mockWallet();
    server.use(http.post("/api/v1/travelers", () => HttpResponse.json({ travelerId: "trav-1" }, { status: 201 })));
    server.use(
      http.post("/api/v1/bookings", () =>
        HttpResponse.json({ title: "Credit limit exceeded" }, { status: 400 })
      )
    );
    renderFlow();

    await waitFor(() => expect(screen.getByRole("heading", { name: "Goa Beach Escape" })).toBeInTheDocument());

    fireEvent.change(screen.getByLabelText(/full name/i), { target: { value: "Jane Traveler" } });
    fireEvent.change(screen.getByLabelText(/date of birth/i), { target: { value: "1990-05-01" } });
    fireEvent.click(screen.getByRole("button", { name: /confirm booking/i }));

    await waitFor(() => {
      expect(screen.getByRole("alert")).toHaveTextContent(/could not complete this booking/i);
    });
  });

  it("shows the credit-limit breach warning when the total would exceed the wallet's headroom", async () => {
    server.use(http.get("/api/v1/packages/pkg-1", () => HttpResponse.json(PACKAGE)));
    mockWallet({ availableBalance: "0.00", creditLimit: "0.00" });
    renderFlow();

    await waitFor(() => {
      expect(screen.getByText(/this booking would exceed your credit limit/i)).toBeInTheDocument();
    });
  });
});
