import { cn } from "@/shared/design-system/cn";
import type { LocationResult } from "./useMultiLocationSearch";

export interface MapPanelProps {
  locations: LocationResult[];
}

// Matches GeocodingService's placeholder India bounding box
// (backend/.../booking/internal/GeocodingService.java) — no real
// geocoding/map provider is named in the PRD, so pin placement is a
// normalized position within the same illustrative range the backend uses,
// not a real basemap. Swap both together if a real provider is adopted.
const LAT_MIN = 8;
const LAT_MAX = 35;
const LNG_MIN = 68;
const LNG_MAX = 97;

/**
 * PRD §9.1 Flow A / §21.1 — one pin per searched location, including one
 * with no inventory (T1), styled distinctly rather than omitted.
 */
export function MapPanel({ locations }: MapPanelProps) {
  if (locations.length === 0) {
    return null;
  }

  return (
    <div
      role="img"
      aria-label={`Map showing ${locations.length} searched location${locations.length === 1 ? "" : "s"}`}
      className="relative h-64 w-full overflow-hidden rounded-md border border-neutral-200 bg-neutral-50"
    >
      {locations.map((location) => {
        const left = ((location.longitude - LNG_MIN) / (LNG_MAX - LNG_MIN)) * 100;
        const top = 100 - ((location.latitude - LAT_MIN) / (LAT_MAX - LAT_MIN)) * 100;
        return (
          <div
            key={location.locationCode}
            data-testid="map-pin"
            data-has-inventory={location.hasInventory}
            style={{ left: `${left}%`, top: `${top}%` }}
            className="absolute flex -translate-x-1/2 -translate-y-full flex-col items-center"
          >
            <span
              aria-hidden="true"
              className={cn(
                "block h-3 w-3 rounded-full border-2 border-white shadow",
                location.hasInventory ? "bg-primary-600" : "bg-neutral-400"
              )}
            />
            <span className="mt-1 whitespace-nowrap text-xs text-neutral-700">
              {location.displayName}
              {!location.hasInventory && " (no inventory)"}
            </span>
          </div>
        );
      })}
    </div>
  );
}
