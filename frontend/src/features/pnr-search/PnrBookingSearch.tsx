import { useState, type FormEvent } from "react";
import { Badge } from "@/shared/design-system/Badge";
import { Button } from "@/shared/design-system/Button";
import { usePnrBookingSearch, type BookingSearchResultView } from "./usePnrBookingSearch";

/**
 * PRD §21.9, HRD-08 — a User types a PNR/booking reference and sees a
 * summary regardless of product type (HRD-07's own AC — {@code Booking}
 * itself carries no product-type field), with a click-through to the full
 * detail: every field the backend has on this booking, expanded inline.
 * All 5 PRD Part 21 states.
 */
export function PnrBookingSearch() {
  const [ref, setRef] = useState("");
  const [expandedBookingId, setExpandedBookingId] = useState<string | null>(null);
  const search = usePnrBookingSearch();

  function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setExpandedBookingId(null);
    search.mutate(ref);
  }

  return (
    <main className="mx-auto max-w-3xl px-6 py-8">
      <h1 className="text-2xl font-semibold text-neutral-900">PNR / Booking Search</h1>
      <p className="mt-1 text-sm text-neutral-600">
        Search by booking reference — results appear regardless of whether it's a hotel, flight, transfer, cruise, or
        activity booking.
      </p>

      <form onSubmit={handleSubmit} className="mt-6 flex items-end gap-3">
        <div className="flex-1">
          <label htmlFor="pnr-ref-input" className="mb-1 block text-sm font-medium text-neutral-700">
            Booking reference
          </label>
          <input
            id="pnr-ref-input"
            required
            value={ref}
            onChange={(e) => setRef(e.target.value)}
            placeholder="ABCD1234"
            className="h-10 w-full rounded-md border border-neutral-300 bg-surface px-3 text-base text-neutral-900 focus:outline-none focus-visible:ring-2 focus-visible:ring-focus-ring focus-visible:ring-offset-2"
          />
        </div>
        <Button type="submit" disabled={search.isPending}>
          {search.isPending ? "Searching…" : "Search"}
        </Button>
      </form>

      <div className="mt-8">
        {search.isPending && (
          <p role="status" className="text-sm text-neutral-600">
            Searching…
          </p>
        )}

        {search.isError && (
          <div
            role="alert"
            className="flex items-center justify-between rounded-md border border-error-600/20 bg-error-50 px-4 py-3"
          >
            <p className="text-sm text-error-700">Could not search for this booking.</p>
            <Button variant="secondary" size="sm" onClick={() => search.mutate(ref)}>
              Retry
            </Button>
          </div>
        )}

        {search.isSuccess && search.data.length === 0 && (
          <p role="status" className="text-sm text-neutral-600">
            No booking found for that reference.
          </p>
        )}

        {search.isSuccess && search.data.length > 0 && (
          <ul aria-label="booking-search-results" className="space-y-3">
            {search.data.map((booking) => (
              <li key={booking.bookingId} className="rounded-md border border-neutral-200 bg-surface px-4 py-3">
                <button
                  type="button"
                  onClick={() => setExpandedBookingId((current) => (current === booking.bookingId ? null : booking.bookingId))}
                  className="flex w-full items-center justify-between text-left"
                >
                  <div>
                    <p className="text-base text-neutral-900">{booking.pnrSearchableRef}</p>
                    <p className="text-sm text-neutral-600">
                      {booking.totalSellPrice.amount} {booking.totalSellPrice.currency}
                    </p>
                  </div>
                  <Badge tone={booking.status === "CONFIRMED" ? "success" : "neutral"}>{booking.status}</Badge>
                </button>

                {expandedBookingId === booking.bookingId && (
                  <BookingDetail booking={booking} />
                )}
              </li>
            ))}
          </ul>
        )}
      </div>
    </main>
  );
}

function BookingDetail({ booking }: { booking: BookingSearchResultView }) {
  return (
    <dl className="mt-3 grid grid-cols-2 gap-x-4 gap-y-1 border-t border-neutral-200 pt-3 text-sm">
      <dt className="text-neutral-600">Booking ID</dt>
      <dd className="text-neutral-900">{booking.bookingId}</dd>
      <dt className="text-neutral-600">Payment method</dt>
      <dd className="text-neutral-900">{booking.paymentMethod}</dd>
      <dt className="text-neutral-600">Created</dt>
      <dd className="text-neutral-900">{new Date(booking.createdAt).toLocaleString()}</dd>
    </dl>
  );
}
