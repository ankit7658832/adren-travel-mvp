import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { describe, expect, it, vi, beforeEach, afterEach } from "vitest";
import { http, HttpResponse } from "msw";
import { BookingConfirmation } from "./BookingConfirmation";
import { server } from "@/test/mswServer";

const BOOKING = {
  bookingId: "booking-1",
  pnrSearchableRef: "ABC12345",
  status: "CONFIRMED",
  paymentMethod: "WALLET",
  totalSellPrice: { amount: "11500.00", currency: "INR" },
  createdAt: "2026-01-15T10:00:00Z",
  voucher: { pdfReference: "vouchers/booking-1/x.pdf", atolCertificateReference: null, generatedAt: "2026-01-15T10:00:01Z" },
};

function renderScreen() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={["/bookings/booking-1/confirmation"]}>
        <Routes>
          <Route path="/bookings/:bookingId/confirmation" element={<BookingConfirmation />} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>
  );
}

describe("BookingConfirmation (SCR-17)", () => {
  beforeEach(() => {
    // jsdom doesn't implement Blob URL creation/revocation.
    window.URL.createObjectURL = vi.fn(() => "blob:mock-url");
    window.URL.revokeObjectURL = vi.fn();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("loading state: shows a loading message while fetching the booking", () => {
    server.use(http.get("/api/v1/bookings/booking-1", () => new Promise<Response>(() => {})));
    renderScreen();

    expect(screen.getByRole("status")).toHaveTextContent(/loading your booking/i);
  });

  it("error state: shows a retry option when the booking fetch fails", async () => {
    server.use(http.get("/api/v1/bookings/booking-1", () => HttpResponse.json({ title: "not found" }, { status: 404 })));
    renderScreen();

    await waitFor(() => {
      expect(screen.getByRole("alert")).toHaveTextContent(/could not load this booking/i);
    });
    expect(screen.getByRole("button", { name: /retry/i })).toBeInTheDocument();
  });

  it("shows the PNR reference, total paid, and no ATOL button when not applicable", async () => {
    server.use(http.get("/api/v1/bookings/booking-1", () => HttpResponse.json(BOOKING)));
    renderScreen();

    await waitFor(() => {
      expect(screen.getByRole("heading", { name: /booking confirmed/i })).toBeInTheDocument();
    });
    expect(screen.getByText("ABC12345")).toBeInTheDocument();
    expect(screen.getByText("INR 11500.00")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: /download atol certificate/i })).not.toBeInTheDocument();
  });

  it("shows the ATOL certificate button when applicable", async () => {
    server.use(
      http.get("/api/v1/bookings/booking-1", () =>
        HttpResponse.json({ ...BOOKING, voucher: { ...BOOKING.voucher, atolCertificateReference: "ATOL-123" } })
      )
    );
    renderScreen();

    await waitFor(() => {
      expect(screen.getByRole("button", { name: /download atol certificate/i })).toBeInTheDocument();
    });
  });

  it("downloads the voucher as a PDF blob when clicked", async () => {
    server.use(http.get("/api/v1/bookings/booking-1", () => HttpResponse.json(BOOKING)));
    server.use(
      http.get("/api/v1/bookings/booking-1/voucher", () =>
        HttpResponse.arrayBuffer(new TextEncoder().encode("PDF content").buffer, {
          headers: { "Content-Type": "application/pdf" },
        })
      )
    );
    renderScreen();

    await waitFor(() => expect(screen.getByRole("heading", { name: /booking confirmed/i })).toBeInTheDocument());
    fireEvent.click(screen.getByRole("button", { name: /download voucher/i }));

    await waitFor(() => {
      expect(window.URL.createObjectURL).toHaveBeenCalled();
    });
  });
});
