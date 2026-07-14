import { Badge } from "@/shared/design-system/Badge";
import { Button } from "@/shared/design-system/Button";
import { useConsultantList, useUpdateConsultantStatus } from "./useConsultantList";

/**
 * PRD §3.1/§21.6 — Super Admin's Consultants list: view every onboarded
 * Consultant and suspend/reinstate them (FND-05). All 5 PRD Part 21 states
 * (default/loading/success/empty/error) per RULES.md §7.2.
 */
export function ConsultantList() {
  const consultantsQuery = useConsultantList();
  const updateStatusMutation = useUpdateConsultantStatus();

  return (
    <main className="mx-auto max-w-3xl px-6 py-8">
      <h1 className="text-2xl font-semibold text-neutral-900">Consultants</h1>

      {updateStatusMutation.isError && (
        <p role="alert" className="mt-2 text-sm text-error-700">
          Could not update this Consultant's status.
        </p>
      )}

      <div className="mt-6">
        {consultantsQuery.isLoading && (
          <p role="status" className="text-sm text-neutral-600">
            Loading consultants…
          </p>
        )}

        {consultantsQuery.isError && (
          <div role="alert" className="flex items-center justify-between rounded-md border border-error-600/20 bg-error-50 px-4 py-3">
            <p className="text-sm text-error-700">Could not load consultants.</p>
            <Button variant="secondary" size="sm" onClick={() => consultantsQuery.refetch()}>
              Retry
            </Button>
          </div>
        )}

        {consultantsQuery.isSuccess && consultantsQuery.data.content.length === 0 && (
          <p className="text-sm text-neutral-600">No consultants onboarded yet.</p>
        )}

        {consultantsQuery.isSuccess && consultantsQuery.data.content.length > 0 && (
          <ul aria-label="consultant-list" className="space-y-3">
            {consultantsQuery.data.content.map((consultant) => (
              <li
                key={consultant.consultantId}
                className="flex items-center justify-between rounded-md border border-neutral-200 bg-surface px-4 py-3"
              >
                <div>
                  <p className="text-base text-neutral-900">{consultant.businessName}</p>
                  <p className="text-sm text-neutral-600">
                    {consultant.homeMarket} · onboarded {new Date(consultant.createdAt).toLocaleDateString()}
                  </p>
                </div>
                <div className="flex items-center gap-3">
                  <Badge tone={consultant.status === "ACTIVE" ? "success" : "error"}>{consultant.status}</Badge>
                  <Button
                    variant={consultant.status === "ACTIVE" ? "destructive" : "secondary"}
                    size="sm"
                    disabled={updateStatusMutation.isPending}
                    onClick={() =>
                      updateStatusMutation.mutate({
                        consultantId: consultant.consultantId,
                        status: consultant.status === "ACTIVE" ? "SUSPENDED" : "ACTIVE",
                      })
                    }
                  >
                    {consultant.status === "ACTIVE" ? "Suspend" : "Reinstate"}
                  </Button>
                </div>
              </li>
            ))}
          </ul>
        )}
      </div>
    </main>
  );
}
