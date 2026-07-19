import { Badge } from "@/shared/design-system/Badge";
import { Button } from "@/shared/design-system/Button";
import { useDisputeTickets } from "./useDisputeTickets";

/**
 * PRD §12.5, HRD-06 — a flagged dispute is visible here as a trackable
 * ticket with a status, for both the Consultant and Super Admin, not just
 * an emailed notice. All 5 PRD Part 21 states.
 */
export function DisputeTicketTracker() {
  const ticketsQuery = useDisputeTickets();

  return (
    <main className="mx-auto max-w-3xl px-6 py-8">
      <h1 className="text-2xl font-semibold text-neutral-900">Dispute Tickets</h1>

      <div className="mt-6">
        {ticketsQuery.isLoading && (
          <p role="status" className="text-sm text-neutral-600">
            Loading dispute tickets…
          </p>
        )}

        {ticketsQuery.isError && (
          <div
            role="alert"
            className="flex items-center justify-between rounded-md border border-error-600/20 bg-error-50 px-4 py-3"
          >
            <p className="text-sm text-error-700">Could not load dispute tickets.</p>
            <Button variant="secondary" size="sm" onClick={() => ticketsQuery.refetch()}>
              Retry
            </Button>
          </div>
        )}

        {ticketsQuery.isSuccess && ticketsQuery.data.content.length === 0 && (
          <p className="text-sm text-neutral-600">No dispute tickets — nothing to track right now.</p>
        )}

        {ticketsQuery.isSuccess && ticketsQuery.data.content.length > 0 && (
          <ul aria-label="dispute-ticket-list" className="space-y-3">
            {ticketsQuery.data.content.map((ticket) => (
              <li
                key={ticket.disputeTicketId}
                className="flex items-center justify-between rounded-md border border-neutral-200 bg-surface px-4 py-3"
              >
                <div>
                  <p className="text-base text-neutral-900">{ticket.reason}</p>
                  <p className="text-sm text-neutral-600">Booking {ticket.bookingId}</p>
                </div>
                <Badge tone={ticket.status === "OPEN" ? "warning" : "success"}>{ticket.status}</Badge>
              </li>
            ))}
          </ul>
        )}
      </div>
    </main>
  );
}
