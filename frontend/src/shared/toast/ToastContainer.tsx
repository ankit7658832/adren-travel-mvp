import { useEffect } from "react";
import { cn } from "@/shared/design-system/cn";
import { useToastQueueStore, type Toast } from "./toastQueueStore";

const AUTO_DISMISS_MS = 5000;

/**
 * FES-10 — the one rendering site for `toastQueueStore`'s queue, mounted
 * once app-wide (AppProviders, FES-02's slot) so any screen's mutation
 * callback can call `useToastQueueStore.getState().addToast(...)` without
 * mounting its own toast UI. `role="alert"` for error tone / `role="status"`
 * for success tone, per RULES.md §7.3's ARIA-live-region baseline — a
 * screen reader announces the result without the user needing focus on
 * this corner of the screen.
 */
export function ToastContainer() {
  const toasts = useToastQueueStore((state) => state.toasts);

  return (
    <div className="pointer-events-none fixed bottom-4 right-4 z-50 flex flex-col gap-2">
      {toasts.map((toast) => (
        <ToastItem key={toast.id} toast={toast} />
      ))}
    </div>
  );
}

function ToastItem({ toast }: { toast: Toast }) {
  // Read removeToast directly (a stable Zustand action reference) rather
  // than taking an onDismiss prop built from an inline arrow function —
  // an inline callback's identity changes on every ToastContainer render
  // (e.g. when a second toast is queued), which would re-run this effect
  // and reset THIS toast's already-ticking timer for no reason.
  const removeToast = useToastQueueStore((state) => state.removeToast);

  useEffect(() => {
    const timer = setTimeout(() => removeToast(toast.id), AUTO_DISMISS_MS);
    return () => clearTimeout(timer);
  }, [toast.id, removeToast]);

  return (
    <div
      role={toast.tone === "error" ? "alert" : "status"}
      className={cn(
        "pointer-events-auto rounded-md px-4 py-3 text-sm shadow-md",
        toast.tone === "error" ? "bg-error-50 text-error-700" : "bg-success-50 text-success-700"
      )}
    >
      {toast.message}
    </div>
  );
}
