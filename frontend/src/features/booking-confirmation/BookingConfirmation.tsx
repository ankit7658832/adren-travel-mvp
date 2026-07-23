import { useParams, Link } from "react-router-dom";
import { CheckCircle2 } from "lucide-react";
import { Button } from "@/shared/design-system/Button";
import { Card } from "@/shared/design-system/Card";
import { useBookingConfirmation, useDownloadVoucher } from "./useBookingConfirmation";

/**
 * SCR-17 (doc/ADREN_UIUX_SPEC.md §12.2) — the payoff screen after {@link
 * import("../booking-payment-flow/BookingPaymentFlow").BookingPaymentFlow}
 * confirms a booking. "Download ATOL Certificate" is per-spec never
 * rendered when not applicable (never a disabled/grayed button) — {@code
 * atolCertificateReference} is always null in this mock phase (see
 * backend VoucherService's own Javadoc), so it never renders today.
 */
export function BookingConfirmation() {
  const { bookingId } = useParams<{ bookingId: string }>();
  const bookingQuery = useBookingConfirmation(bookingId);
  const downloadVoucher = useDownloadVoucher();

  if (bookingQuery.isLoading) {
    return (
      <main className="mx-auto max-w-2xl px-6 py-8">
        <p role="status" className="text-sm text-neutral-600">
          Loading your booking…
        </p>
      </main>
    );
  }

  if (bookingQuery.isError || !bookingQuery.data) {
    return (
      <main className="mx-auto max-w-2xl px-6 py-8">
        <div role="alert" className="flex items-center justify-between rounded-md border border-error-600/20 bg-error-50 px-4 py-3">
          <p className="text-sm text-error-700">Could not load this booking.</p>
          <Button variant="secondary" size="sm" onClick={() => bookingQuery.refetch()}>
            Retry
          </Button>
        </div>
      </main>
    );
  }

  const booking = bookingQuery.data;

  return (
    <main className="mx-auto max-w-2xl px-6 py-8 text-center">
      <CheckCircle2 aria-hidden="true" className="mx-auto h-16 w-16 text-success-500" />
      <h1 className="mt-4 text-2xl font-semibold text-neutral-900">Booking Confirmed</h1>
      <p className="mt-2 font-mono text-lg text-neutral-700">{booking.pnrSearchableRef}</p>

      <Card className="mt-6 text-left">
        <dl className="space-y-2 text-sm">
          <div className="flex justify-between">
            <dt className="text-neutral-600">Total paid</dt>
            <dd className="font-medium text-neutral-900">
              {booking.totalSellPrice.currency} {Number(booking.totalSellPrice.amount).toFixed(2)}
            </dd>
          </div>
          <div className="flex justify-between">
            <dt className="text-neutral-600">Payment method</dt>
            <dd className="font-medium text-neutral-900">{booking.paymentMethod}</dd>
          </div>
          <div className="flex justify-between">
            <dt className="text-neutral-600">Confirmed</dt>
            <dd className="font-medium text-neutral-900">{new Date(booking.createdAt).toLocaleString()}</dd>
          </div>
        </dl>
      </Card>

      {downloadVoucher.isError && (
        <p role="alert" className="mt-4 text-sm text-error-700">
          Could not download the voucher. Please try again.
        </p>
      )}

      <div className="mt-6 flex flex-col items-center gap-3">
        <Button
          onClick={() => downloadVoucher.mutate(booking.bookingId)}
          disabled={downloadVoucher.isPending}
          className="w-full max-w-xs"
        >
          {downloadVoucher.isPending ? "Preparing…" : "Download Voucher"}
        </Button>
        {booking.voucher.atolCertificateReference && (
          <Button variant="secondary" className="w-full max-w-xs">
            Download ATOL Certificate
          </Button>
        )}
        <Link to="/pnr" className="text-sm text-primary-600 hover:underline">
          View Booking
        </Link>
      </div>
    </main>
  );
}
