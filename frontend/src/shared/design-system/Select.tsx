/**
 * doc/DESIGN.md §7 "Form inputs" spec, same fixed border/focus/error
 * treatment as TextField. Native `<select>`, not a Radix combobox — this
 * primitive covers plain single-choice dropdowns; DESIGN.md's own
 * Radix-Select recommendation targets interaction complexity (the Search
 * Dashboard's multiselect-autocomplete location box already exists as its
 * own feature-specific component) this generic primitive doesn't need,
 * and native `<select>` already ships full keyboard/screen-reader support
 * with no extra dependency.
 */
import { type SelectHTMLAttributes, forwardRef, useId } from "react";
import { cn } from "./cn";

export interface SelectOption {
  value: string;
  label: string;
}

export interface SelectProps extends SelectHTMLAttributes<HTMLSelectElement> {
  label: string;
  options: SelectOption[];
  error?: string;
}

export const Select = forwardRef<HTMLSelectElement, SelectProps>(
  ({ label, options, error, id, className, ...props }, ref) => {
    const generatedId = useId();
    const selectId = id ?? generatedId;
    const errorId = `${selectId}-error`;

    return (
      <div className="flex flex-col gap-1">
        <label htmlFor={selectId} className="text-sm font-medium text-neutral-900">
          {label}
        </label>
        <select
          ref={ref}
          id={selectId}
          aria-invalid={error ? true : undefined}
          aria-describedby={error ? errorId : undefined}
          className={cn(
            "h-10 rounded-md border bg-surface px-3 text-base text-neutral-900 transition-colors duration-standard",
            "focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-focus-ring focus-visible:ring-offset-2",
            error
              ? "border-error-600 bg-error-50"
              : "border-neutral-300 focus:border-primary-600",
            className
          )}
          {...props}
        >
          {options.map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </select>
        {error ? (
          <p id={errorId} role="alert" className="text-sm text-error-700">
            {error}
          </p>
        ) : null}
      </div>
    );
  }
);
Select.displayName = "Select";
