import { ErrorBoundary, type FallbackProps } from "react-error-boundary";
import type { ReactNode } from "react";

/**
 * Last line of defense (RULES.md §7.4) — wraps the router in main.tsx.
 * Fallback is generic and severe on purpose: by definition something
 * escaped every more specific per-route boundary. Dependency-light (no
 * store/query reads) so a corrupted app-wide store can't also crash the
 * fallback itself.
 */
function RootFallback({ error }: FallbackProps) {
  return (
    <div role="alert" className="flex min-h-screen flex-col items-center justify-center gap-4 px-6 text-center">
      <h1 className="text-xl font-semibold text-neutral-900">Something went wrong</h1>
      <p className="text-sm text-neutral-600">
        {error instanceof Error ? error.message : "An unexpected error occurred."}
      </p>
      <button
        type="button"
        onClick={() => window.location.reload()}
        className="rounded-md bg-primary-600 px-4 py-2 text-sm font-medium text-white hover:bg-primary-700"
      >
        Reload
      </button>
    </div>
  );
}

export function RootErrorBoundary({ children }: { children: ReactNode }) {
  return <ErrorBoundary FallbackComponent={RootFallback}>{children}</ErrorBoundary>;
}
