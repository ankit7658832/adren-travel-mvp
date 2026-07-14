/**
 * Sample BrandingProfile data standing in for what a real
 * `GET /api/v1/consultants/{id}/branding` (FND-06) would return. There is
 * no backend endpoint for this yet — these presets exist purely to
 * demonstrate doc/DESIGN.md §3.3's contrast-safety algorithm actually
 * running against a range of realistic Consultant color/image picks:
 * a safe pick, a pick a scrim can rescue, and a pick nothing can rescue.
 */
import type { BrandingProfile } from "@/shared/theming/resolveTenantTheme";

export interface BrandingPreset {
  id: string;
  label: string;
  description: string;
  profile: BrandingProfile;
  /** If set, backgroundImageUrl is synthesized via canvas at runtime (doc/DESIGN.md §3.3 step 1's image-sampling path) as a gradient between these two stops, instead of using a flat color. */
  generatedImageStops?: [string, string];
}

export const BRANDING_PRESETS: BrandingPreset[] = [
  {
    id: "safe",
    label: "Nordic Fjord Journeys — safe pick",
    description:
      "Navy text on a light warm background. Passes AA outright, no scrim or fallback needed.",
    profile: {
      consultantName: "Nordic Fjord Journeys",
      logoUrl: null,
      backgroundImageUrl: null,
      backgroundColor: "#F4F1EA",
      textColorPrimary: "#1F2A44",
      textColorSecondary: "#1F2A44",
    },
  },
  {
    id: "scrim-rescue",
    label: "Desert Dune Safaris — scrim-rescued pick",
    description:
      "A warm sunset photo (generated on the fly) with a dark-brown text pick that fails outright over the photo, but a semi-transparent scrim brings it back to AA.",
    generatedImageStops: ["#E8A15C", "#8B3A1A"],
    profile: {
      consultantName: "Desert Dune Safaris",
      logoUrl: null,
      backgroundImageUrl: null, // filled in at runtime via generateSampleGradientImage
      backgroundColor: "#C97B3D",
      textColorPrimary: "#5C3A1E",
      textColorSecondary: "#5C3A1E",
    },
  },
  {
    id: "fallback-required",
    label: "Coastal Drift Co — fallback-required pick",
    description:
      "Two near-identical mid-tone greens. No scrim opacity within the cap can separate them — the system auto-substitutes a safe color and the picker would block save.",
    profile: {
      consultantName: "Coastal Drift Co",
      logoUrl: null,
      backgroundImageUrl: null,
      backgroundColor: "#7F9A8C",
      textColorPrimary: "#84A395",
      textColorSecondary: "#84A395",
    },
  },
];
