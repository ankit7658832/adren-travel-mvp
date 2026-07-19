/**
 * doc/DESIGN.md §7 — Card spec: surface background, neutral-200 1px
 * border, 8px radius, space-4/space-6 internal padding. Fixed styling on
 * both layers — on Layer 2 (quotation/voucher line items) only the page
 * chrome around a card is themed, never the card content itself.
 */
import type { HTMLAttributes } from "react";
import { cva, type VariantProps } from "class-variance-authority";
import { cn } from "./cn";

const cardVariants = cva("bg-surface border border-neutral-200 rounded-lg", {
  variants: {
    padding: {
      sm: "p-4",
      md: "p-6",
    },
  },
  defaultVariants: {
    padding: "md",
  },
});

export interface CardProps
  extends HTMLAttributes<HTMLDivElement>,
    VariantProps<typeof cardVariants> {}

export function Card({ className, padding, ...props }: CardProps) {
  return <div className={cn(cardVariants({ padding }), className)} {...props} />;
}
