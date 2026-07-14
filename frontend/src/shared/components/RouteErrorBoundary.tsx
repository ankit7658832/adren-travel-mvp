import { ErrorBoundary, type FallbackProps } from "react-error-boundary";
import { useQueryErrorResetBoundary } from "@tanstack/react-query";
import type { ReactNode } from "react";

/**
 * Per-route boundary (RULES.md §7.4) — one feature crashing (e.g. a bug in
 * a not-yet-built screen) must never take down navigation or another
 * in-progress screen. Paired with `useQueryErrorResetBoundary` so "Retry"
 * re-attempts a failed query instead of just re-rendering the same broken
 * state.
 */
function RouteFallback({ error, resetErrorBoundary }: FallbackProps) {
  return (
    <div role="alert" className="mx-auto max-w-4xl px-6 py-8">
      <h2 className="text-lg font-semibold text-neutral-900">We couldn't load this screen</h2>
      <p className="mt-1 text-sm text-neutral-600">
        {error instanceof Error ? error.message : "An unexpected error occurred."}
      </p>
      <button
        type="button"
        onClick={resetErrorBoundary}
        className="mt-4 rounded-md border border-neutral-300 px-4 py-2 text-sm font-medium text-neutral-900 hover:bg-neutral-50"
      >
        Retry
      </button>
    </div>
  );
}

export function RouteErrorBoundary({ children }: { children: ReactNode }) {
  const { reset } = useQueryErrorResetBoundary();
  return (
    <ErrorBoundary FallbackComponent={RouteFallback} onReset={reset}>
      {children}
    </ErrorBoundary>
  );
}
