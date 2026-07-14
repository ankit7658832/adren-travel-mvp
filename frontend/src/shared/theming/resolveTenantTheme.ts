/**
 * doc/DESIGN.md §3.3, §3.6 — turns a Consultant's raw branding input into
 * the resolved, contrast-safe tokens components are actually allowed to
 * render. Layer 2 components must consume ResolvedTenantTheme, never the
 * raw BrandingProfile fields directly (doc/DESIGN.md §3.3 step 5/6).
 */
import {
  AA_LARGE_TEXT_RATIO,
  AA_NORMAL_TEXT_RATIO,
  resolveSafeTextColor,
  type ResolveSafeTextColorResult,
} from "./contrastSafety";

/**
 * The four tenant-overridable fields per PRD §13.2 / doc/DESIGN.md §3.1,
 * plus the identifying fields the placeholder-logo (§3.5) needs.
 */
export interface BrandingProfile {
  consultantName: string;
  logoUrl: string | null;
  backgroundImageUrl: string | null;
  /** Flat fallback color — always required, used when no image or the image fails to load (doc/DESIGN.md §3.4). */
  backgroundColor: string;
  textColorPrimary: string;
  textColorSecondary: string;
}

/**
 * Effective flat background color for each of the two zones the tenant's
 * text ever renders directly over (doc/DESIGN.md §3.2) — either the sampled
 * average of the uploaded image region (see sampleImageRegion.ts) or, if no
 * image, the flat backgroundColor for both zones.
 */
export interface SampledZoneColors {
  header: string;
  hero: string;
}

export interface ResolvedTenantTheme {
  consultantName: string;
  logoUrl: string | null;
  logoInitials: string;
  backgroundImageUrl: string | null;
  backgroundColor: string;
  /** Nav/header wordmark + tagline text — normal-text AA target. */
  header: ResolveSafeTextColorResult;
  /** Hero headline — large-text AA target (doc/DESIGN.md §4 hero uses --text-4xl). */
  hero: ResolveSafeTextColorResult;
}

function initialsFrom(name: string): string {
  const parts = name.trim().split(/\s+/).filter(Boolean);
  if (parts.length === 0) return "?";
  if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase();
  return (parts[0][0] + parts[1][0]).toUpperCase();
}

export function resolveTenantTheme(
  profile: BrandingProfile,
  sampledZoneColors: SampledZoneColors
): ResolvedTenantTheme {
  return {
    consultantName: profile.consultantName,
    logoUrl: profile.logoUrl,
    logoInitials: initialsFrom(profile.consultantName),
    backgroundImageUrl: profile.backgroundImageUrl,
    backgroundColor: profile.backgroundColor,
    header: resolveSafeTextColor({
      textColor: profile.textColorPrimary,
      backgroundColor: sampledZoneColors.header,
      targetRatio: AA_NORMAL_TEXT_RATIO,
    }),
    hero: resolveSafeTextColor({
      textColor: profile.textColorSecondary,
      backgroundColor: sampledZoneColors.hero,
      targetRatio: AA_LARGE_TEXT_RATIO,
    }),
  };
}
