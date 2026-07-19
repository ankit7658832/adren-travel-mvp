import type { ReactNode } from "react";

export interface ResultsPanelProps {
  map: ReactNode;
  ariaLabel: string;
  children: ReactNode;
}

/**
 * FES-05 — the map+results split-panel shape shared by Search Dashboard
 * (§21.1) and Itinerary Builder (§21.2): map left on desktop, map on top
 * on mobile (a single stacked column, map first in source order — no
 * separate mobile-only markup needed). Owns the split layout itself so
 * neither consuming screen re-implements the responsive grid; `MapPanel`
 * stays an independent, separately testable primitive passed in as a prop
 * rather than being rendered internally.
 */
export function ResultsPanel({ map, ariaLabel, children }: ResultsPanelProps) {
  return (
    <div className="mt-6 flex flex-col gap-6 md:flex-row">
      <div className="md:w-1/2">{map}</div>
      <ul aria-label={ariaLabel} className="flex-1 space-y-3">
        {children}
      </ul>
    </div>
  );
}
