import { type ClassValue, clsx } from "clsx";
import { twMerge } from "tailwind-merge";

/** Standard shadcn-convention class combiner — doc/DESIGN.md §6. */
export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}
