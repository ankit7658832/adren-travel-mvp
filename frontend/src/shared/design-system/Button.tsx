/**
 * doc/DESIGN.md §7 — Button spec. Layer 1 component: also renders on Layer
 * 2 surfaces (storefront CTAs) as the `primary` variant only, always in
 * Adren purple — buttons are never tenant-colored (doc/DESIGN.md §3.1).
 */
import { type ButtonHTMLAttributes, forwardRef } from "react";
import { cva, type VariantProps } from "class-variance-authority";
import { cn } from "./cn";

const buttonVariants = cva(
  "inline-flex items-center justify-center gap-2 rounded-md font-medium " +
    "transition-colors duration-standard focus-visible:outline-none " +
    "focus-visible:ring-2 focus-visible:ring-focus-ring focus-visible:ring-offset-2 " +
    "disabled:pointer-events-none disabled:bg-neutral-200 disabled:text-neutral-400",
  {
    variants: {
      variant: {
        primary: "bg-primary-600 text-white hover:bg-primary-700",
        secondary:
          "bg-surface text-neutral-900 border border-neutral-300 hover:bg-neutral-50",
        ghost: "bg-transparent text-neutral-700 hover:bg-neutral-100",
        destructive: "bg-error-600 text-white hover:bg-error-700",
      },
      size: {
        sm: "h-8 px-3 text-sm",
        md: "h-10 px-4 text-base",
        lg: "h-12 px-6 text-lg",
      },
    },
    defaultVariants: {
      variant: "primary",
      size: "md",
    },
  }
);

export interface ButtonProps
  extends ButtonHTMLAttributes<HTMLButtonElement>,
    VariantProps<typeof buttonVariants> {}

export const Button = forwardRef<HTMLButtonElement, ButtonProps>(
  ({ className, variant, size, ...props }, ref) => (
    <button
      ref={ref}
      className={cn(buttonVariants({ variant, size }), className)}
      {...props}
    />
  )
);
Button.displayName = "Button";
