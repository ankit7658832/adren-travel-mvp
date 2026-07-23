import { useEffect } from "react";
import { AlertCircle } from "lucide-react";
import { Button } from "@/shared/design-system/Button";

export interface ErrorModalProps {
  title?: string;
  message: string;
  onRetry?: () => void;
  onDismiss: () => void;
}

/**
 * SCR-16 (doc/ADREN_UIUX_SPEC.md §2.5) — the blocking error modal for
 * anything that stops the user's current task (a whole search failing,
 * payment failure, no supplier availability at all). Always Layer 1
 * styled regardless of trigger context (doc/DESIGN.md §7's Modal rule) —
 * this component never reads tenant theming. No Radix dependency (none
 * exists anywhere in this codebase despite doc/DESIGN.md §6's
 * recommendation — same hand-built-primitive precedent every other
 * shared component here already follows); Escape-to-dismiss and a click
 * on the scrim are the minimal keyboard/mouse affordances in its place —
 * the scrim is a real <button> (not a div with a click handler) so it's
 * natively focusable/keyboard-operable without a bolted-on role.
 */
export function ErrorModal({ title = "Something went wrong", message, onRetry, onDismiss }: ErrorModalProps) {
  useEffect(() => {
    function handleKeyDown(e: KeyboardEvent) {
      if (e.key === "Escape") {
        onDismiss();
      }
    }
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [onDismiss]);

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center">
      <button
        type="button"
        aria-label="Close error dialog"
        onClick={onDismiss}
        className="absolute inset-0 bg-neutral-900/40"
      />
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby="error-modal-title"
        className="relative w-full max-w-[480px] rounded-lg border-t-4 border-error-600 bg-surface p-6 shadow-lg"
      >
        <AlertCircle aria-hidden="true" className="h-8 w-8 text-error-600" />
        <h3 id="error-modal-title" className="mt-2 text-lg font-semibold text-neutral-900">
          {title}
        </h3>
        <p role="alert" className="mt-2 text-sm text-neutral-700">
          {message}
        </p>
        <div className="mt-6 flex justify-end gap-3">
          <Button variant="ghost" onClick={onDismiss}>
            Dismiss
          </Button>
          {onRetry && <Button onClick={onRetry}>Retry</Button>}
        </div>
      </div>
    </div>
  );
}
