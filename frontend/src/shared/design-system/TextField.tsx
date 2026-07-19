/**
 * doc/DESIGN.md §7 "Form inputs" spec — default border neutral-300, focus
 * border primary-600 + ring, error state always error-600 border +
 * error-50 background + error-700 inline message, fixed regardless of
 * layer (§3.1's explicit carve-out for validation). RULES.md §7.3 baseline:
 * every input needs an associated `<label>` via `htmlFor`/`id`, not
 * placeholder-as-label — this wires `aria-invalid`/`aria-describedby` to
 * the error message automatically so no consumer has to repeat that
 * plumbing (FES-04's own acceptance criterion).
 */
import { type InputHTMLAttributes, forwardRef, useId } from "react";
import { cn } from "./cn";

export interface TextFieldProps extends InputHTMLAttributes<HTMLInputElement> {
  label: string;
  error?: string;
}

export const TextField = forwardRef<HTMLInputElement, TextFieldProps>(
  ({ label, error, id, className, ...props }, ref) => {
    const generatedId = useId();
    const inputId = id ?? generatedId;
    const errorId = `${inputId}-error`;

    return (
      <div className="flex flex-col gap-1">
        <label htmlFor={inputId} className="text-sm font-medium text-neutral-900">
          {label}
        </label>
        <input
          ref={ref}
          id={inputId}
          aria-invalid={error ? true : undefined}
          aria-describedby={error ? errorId : undefined}
          className={cn(
            "h-10 rounded-md border px-3 text-base text-neutral-900 transition-colors duration-standard",
            "focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-focus-ring focus-visible:ring-offset-2",
            error
              ? "border-error-600 bg-error-50"
              : "border-neutral-300 focus:border-primary-600",
            className
          )}
          {...props}
        />
        {error ? (
          <p id={errorId} role="alert" className="text-sm text-error-700">
            {error}
          </p>
        ) : null}
      </div>
    );
  }
);
TextField.displayName = "TextField";
