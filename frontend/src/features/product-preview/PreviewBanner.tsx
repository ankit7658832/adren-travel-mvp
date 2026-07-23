import { AlertTriangle } from "lucide-react";

/** Renders on every SCR-04–13 preview screen — see productPreviewData.ts's own doc comment for why. */
export function PreviewBanner() {
  return (
    <div
      role="note"
      className="flex items-center gap-2 rounded-md border border-warning-500/40 bg-warning-50 px-4 py-2 text-sm text-secondary-900"
    >
      <AlertTriangle aria-hidden="true" className="h-4 w-4 shrink-0" />
      Preview — mock data, not live inventory. No supplier search exists yet for this product category.
    </div>
  );
}
