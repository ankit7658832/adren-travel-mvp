import { cn } from "@/shared/design-system/cn";

export interface MapPin {
  id: string;
  latitude: number;
  longitude: number;
  label: string;
  /** Omitted (not just `true`) means "not applicable" — e.g. an already-selected itinerary line item, not a search result that could lack inventory. */
  hasInventory?: boolean;
}

export interface MapPanelProps {
  pins: MapPin[];
  ariaLabel: string;
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
 * FES-05 — extracted/generalized from FND-13's Search Dashboard
 * implementation (PRD §9.1 Flow A / §21.1) so §21.2's Itinerary Builder can
 * reuse the identical pin-map rendering without re-implementing it.
 * Generic over `MapPin`, not any one screen's server-response shape.
 */
export function MapPanel({ pins, ariaLabel }: MapPanelProps) {
  if (pins.length === 0) {
    return null;
  }

  return (
    <div
      role="img"
      aria-label={ariaLabel}
      className="relative h-64 w-full overflow-hidden rounded-md border border-neutral-200 bg-neutral-50"
    >
      {pins.map((pin) => {
        const left = ((pin.longitude - LNG_MIN) / (LNG_MAX - LNG_MIN)) * 100;
        const top = 100 - ((pin.latitude - LAT_MIN) / (LAT_MAX - LAT_MIN)) * 100;
        return (
          <div
            key={pin.id}
            data-testid="map-pin"
            data-has-inventory={pin.hasInventory}
            style={{ left: `${left}%`, top: `${top}%` }}
            className="absolute flex -translate-x-1/2 -translate-y-full flex-col items-center"
          >
            <span
              aria-hidden="true"
              className={cn(
                "block h-3 w-3 rounded-full border-2 border-white shadow",
                pin.hasInventory === false ? "bg-neutral-400" : "bg-primary-600"
              )}
            />
            <span className="mt-1 whitespace-nowrap text-xs text-neutral-700">
              {pin.label}
              {pin.hasInventory === false && " (no inventory)"}
            </span>
          </div>
        );
      })}
    </div>
  );
}
