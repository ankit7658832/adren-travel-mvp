import { Badge } from "@/shared/design-system/Badge";
import { Button } from "@/shared/design-system/Button";
import { useAiGovernanceLog } from "./useAiGovernanceLog";

const DISPOSITION_TONE: Record<string, "success" | "warning" | "error" | "neutral"> = {
  SUGGESTED: "success",
  NO_VIABLE_SUGGESTION: "warning",
  GROQ_ERROR: "error",
};

/**
 * PRD §6/§21.6, AI-11 — the Super Admin Console's AI Governance/Audit Log
 * viewer: every AI suggestion's input, source data, output, and
 * disposition, paginated and filterable by Consultant. Role-gating is
 * enforced server-side (the backend endpoint is SUPER_ADMIN-only) — this
 * component renders whatever the API returns, including its 403 for any
 * other role. All 5 PRD Part 21 states (default/loading/success/empty/error).
 */
export function AiGovernanceLogViewer() {
  const { query, consultantIdFilter, setConsultantIdFilter, page, setPage } = useAiGovernanceLog();

  return (
    <main className="mx-auto max-w-4xl px-6 py-8">
      <h1 className="text-2xl font-semibold text-neutral-900">AI Governance Logs</h1>

      <div className="mt-4">
        <label htmlFor="consultant-id-filter" className="mb-1 block text-sm font-medium text-neutral-700">
          Filter by Consultant ID
        </label>
        <input
          id="consultant-id-filter"
          value={consultantIdFilter}
          onChange={(e) => setConsultantIdFilter(e.target.value)}
          placeholder="All consultants"
          className="h-10 w-96 rounded-md border border-neutral-300 bg-surface px-3 text-base text-neutral-900"
        />
      </div>

      <div className="mt-6">
        {query.isLoading && (
          <p role="status" className="text-sm text-neutral-600">
            Loading AI governance logs…
          </p>
        )}

        {query.isError && (
          <div
            role="alert"
            className="flex items-center justify-between rounded-md border border-error-600/20 bg-error-50 px-4 py-3"
          >
            <p className="text-sm text-error-700">Could not load AI governance logs.</p>
            <Button variant="secondary" size="sm" onClick={() => query.refetch()}>
              Retry
            </Button>
          </div>
        )}

        {query.isSuccess && query.data.content.length === 0 && (
          <p className="text-sm text-neutral-600">No AI suggestions logged yet.</p>
        )}

        {query.isSuccess && query.data.content.length > 0 && (
          <>
            <ul aria-label="ai-governance-log-entries" className="space-y-3">
              {query.data.content.map((entry) => (
                <li key={entry.auditLogId} className="rounded-md border border-neutral-200 bg-surface px-4 py-3">
                  <div className="flex items-center justify-between">
                    <p className="text-sm text-neutral-600">
                      Consultant {entry.consultantId} · {new Date(entry.createdAt).toLocaleString()}
                    </p>
                    <Badge tone={DISPOSITION_TONE[entry.disposition] ?? "neutral"}>{entry.disposition}</Badge>
                  </div>

                  <details className="mt-2">
                    <summary className="cursor-pointer text-sm font-medium text-neutral-700">Input</summary>
                    <pre className="mt-1 whitespace-pre-wrap break-all text-xs text-neutral-600">
                      {entry.requestInputJson}
                    </pre>
                  </details>
                  <details className="mt-2">
                    <summary className="cursor-pointer text-sm font-medium text-neutral-700">Source data</summary>
                    <pre className="mt-1 whitespace-pre-wrap break-all text-xs text-neutral-600">
                      {entry.sourceDataSnapshotJson}
                    </pre>
                  </details>
                  <details className="mt-2">
                    <summary className="cursor-pointer text-sm font-medium text-neutral-700">Output</summary>
                    <pre className="mt-1 whitespace-pre-wrap break-all text-xs text-neutral-600">
                      {entry.aiOutputJson ?? "(none)"}
                    </pre>
                  </details>
                </li>
              ))}
            </ul>

            <div className="mt-4 flex items-center justify-between">
              <p className="text-sm text-neutral-600">
                Page {query.data.page + 1} of {query.data.totalPages} · {query.data.totalElements} total
              </p>
              <div className="flex gap-2">
                <Button
                  variant="secondary"
                  size="sm"
                  disabled={page === 0}
                  onClick={() => setPage((p) => Math.max(0, p - 1))}
                >
                  Previous
                </Button>
                <Button
                  variant="secondary"
                  size="sm"
                  disabled={query.data.page + 1 >= query.data.totalPages}
                  onClick={() => setPage((p) => p + 1)}
                >
                  Next
                </Button>
              </div>
            </div>
          </>
        )}
      </div>
    </main>
  );
}
