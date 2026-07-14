/**
 * doc/DESIGN.md §2.2, §7 — status badge. Takes a semantic `tone`, never a
 * raw color prop, so the AA-safe fill+text pairings from doc/DESIGN.md §2.2
 * can't accidentally be wired back to a failing raw-brief-color-on-white
 * pairing by a call site. Layer 1 only — status enums are internal,
 * never shown to the End Traveler (doc/DESIGN.md §7).
 */
import type { ReactNode } from "react";
import { cva, type VariantProps } from "class-variance-authority";
import { cn } from "./cn";

const badgeVariants = cva(
  "inline-flex items-center rounded-full px-3 py-1 text-xs font-medium",
  {
    variants: {
      tone: {
        neutral: "bg-neutral-100 text-neutral-700",
        success: "bg-success-50 text-success-700",
        warning: "bg-warning-500 text-secondary-900", // fill + navy text, doc/DESIGN.md §2.2 — 7.84:1
        info: "bg-info-50 text-info-700",
        error: "bg-error-50 text-error-700",
      },
    },
    defaultVariants: {
      tone: "neutral",
    },
  }
);

export interface BadgeProps extends VariantProps<typeof badgeVariants> {
  children: ReactNode;
  className?: string;
}

export function Badge({ tone, children, className }: BadgeProps) {
  return <span className={cn(badgeVariants({ tone }), className)}>{children}</span>;
}
