import { useEffect, useMemo, useState } from "react";
import { Button } from "@/shared/design-system/Button";
import { Badge } from "@/shared/design-system/Badge";
import { TenantThemedSurface } from "@/shared/theming/TenantThemedSurface";
import { useTenantThemeStore } from "@/shared/theming/tenantThemeStore";
import { resolveTenantTheme } from "@/shared/theming/resolveTenantTheme";
import {
  HEADER_BAND_REGION,
  HERO_SAFE_ZONE_REGION,
  sampleAverageColor,
} from "@/shared/theming/sampleImageRegion";
import { generateSampleGradientImage } from "./generateSampleGradientImage";
import { BRANDING_PRESETS } from "./sampleBrandingPresets";

/**
 * Placeholder Layer 2 screen — doc/DESIGN.md §10, §12 item 4. PRD Part 21
 * has no dedicated screen spec for the Consultant storefront/quotation
 * surface the End Traveler actually sees (persona 3.4, PRD §13.2); this is
 * a provisional stand-in whose only job is to demonstrate doc/DESIGN.md
 * §3.3's contrast-safety algorithm running end-to-end against a sample
 * uploaded background/color — not a final storefront design.
 *
 * There is no backend branding endpoint yet (FND-06/FND-07 are backend
 * stories, not built here) — sampleBrandingPresets.ts stands in for what
 * `GET /api/v1/consultants/{id}/branding` would return.
 */
export function ConsultantStorefront() {
  const [selectedPresetId, setSelectedPresetId] = useState(
    BRANDING_PRESETS[0].id
  );
  const preset =
    BRANDING_PRESETS.find((p) => p.id === selectedPresetId) ??
    BRANDING_PRESETS[0];

  const [backgroundImageUrl, setBackgroundImageUrl] = useState<string | null>(
    null
  );
  const [sampledZoneColors, setSampledZoneColors] = useState({
    header: preset.profile.backgroundColor,
    hero: preset.profile.backgroundColor,
  });

  const setActiveTheme = useTenantThemeStore((state) => state.setActiveTheme);

  // doc/DESIGN.md §3.3 step 1: sample the effective background color per
  // zone. Flat backgroundColor needs no sampling; an uploaded image is
  // sampled via canvas. Falls back to the flat color on any failure —
  // the same fallback doc/DESIGN.md §3.4 specifies for a failed image load.
  useEffect(() => {
    let cancelled = false;

    async function resolveZoneColors() {
      if (!preset.generatedImageStops) {
        setBackgroundImageUrl(null);
        setSampledZoneColors({
          header: preset.profile.backgroundColor,
          hero: preset.profile.backgroundColor,
        });
        return;
      }

      try {
        const imageUrl = generateSampleGradientImage(
          ...preset.generatedImageStops
        );
        if (!imageUrl) throw new Error("canvas unavailable");
        if (cancelled) return;
        setBackgroundImageUrl(imageUrl);

        const [header, hero] = await Promise.all([
          sampleAverageColor(imageUrl, HEADER_BAND_REGION),
          sampleAverageColor(imageUrl, HERO_SAFE_ZONE_REGION),
        ]);
        if (!cancelled) setSampledZoneColors({ header, hero });
      } catch {
        // doc/DESIGN.md §3.4 — image failed to load/sample: fall back to
        // the flat backgroundColor rather than leaving text unresolved.
        if (!cancelled) {
          setBackgroundImageUrl(null);
          setSampledZoneColors({
            header: preset.profile.backgroundColor,
            hero: preset.profile.backgroundColor,
          });
        }
      }
    }

    resolveZoneColors();
    return () => {
      cancelled = true;
    };
  }, [preset]);

  const resolvedTheme = useMemo(
    () =>
      resolveTenantTheme(
        { ...preset.profile, backgroundImageUrl },
        sampledZoneColors
      ),
    [preset, backgroundImageUrl, sampledZoneColors]
  );

  useEffect(() => {
    setActiveTheme(resolvedTheme);
    return () => setActiveTheme(null);
  }, [resolvedTheme, setActiveTheme]);

  const saveBlocked =
    resolvedTheme.header.textColorSource === "auto-fallback" ||
    resolvedTheme.hero.textColorSource === "auto-fallback";

  return (
    <main className="mx-auto max-w-6xl px-6 py-8">
      <h1 className="text-2xl font-semibold text-neutral-900">
        Consultant Storefront — branding preview
      </h1>
      <p className="mt-1 text-sm text-neutral-600">
        Layer 2 demo per doc/DESIGN.md §3 &amp; §12 item 4 — a live preview +
        validation surface (§3.6) next to the actual themed page it
        describes.
      </p>

      <div className="mt-6 grid grid-cols-1 gap-6 lg:grid-cols-[360px_1fr]">
        {/* Left: Super Admin / Consultant branding picker + live validation, doc/DESIGN.md §3.6 */}
        <div className="space-y-4 rounded-md border border-neutral-200 bg-surface p-4">
          <h2 className="text-sm font-semibold text-neutral-900">
            Branding config (sample presets)
          </h2>

          <div className="space-y-2">
            {BRANDING_PRESETS.map((p) => (
              <label
                key={p.id}
                className="flex cursor-pointer items-start gap-2 rounded-md border border-neutral-200 p-2 text-sm hover:bg-neutral-50 has-[:checked]:border-primary-600 has-[:checked]:bg-primary-50"
              >
                <input
                  type="radio"
                  name="preset"
                  className="mt-1"
                  checked={p.id === selectedPresetId}
                  onChange={() => setSelectedPresetId(p.id)}
                />
                <span>
                  <span className="block font-medium text-neutral-900">
                    {p.label}
                  </span>
                  <span className="block text-neutral-600">
                    {p.description}
                  </span>
                </span>
              </label>
            ))}
          </div>

          <ValidationReadout label="Header zone" result={resolvedTheme.header} />
          <ValidationReadout label="Hero zone" result={resolvedTheme.hero} />

          <div className="pt-2">
            <Button className="w-full" disabled={saveBlocked}>
              Save branding
            </Button>
            {saveBlocked && (
              <p
                role="alert"
                className="mt-2 rounded-md bg-error-50 px-3 py-2 text-xs text-error-700"
              >
                Even with a scrim, one of your text colors isn&apos;t safe
                here — save is blocked. We&apos;ll use a safe fallback color
                unless you pick a different text color or background (doc/DESIGN.md
                §3.3 fallback chain).
              </p>
            )}
          </div>
        </div>

        {/* Right: the actual themed page, doc/DESIGN.md §3.6 "real sample page, not swatches" */}
        <TenantThemedSurface
          theme={resolvedTheme}
          className="overflow-hidden rounded-md border border-neutral-200"
        >
          <header
            className="flex items-center gap-3 px-6 py-3"
            style={{
              backgroundColor: "var(--tenant-bg-color)",
              backgroundImage: "var(--tenant-bg-image)",
              backgroundSize: "cover",
              backgroundPosition: "center top",
              position: "relative",
            }}
          >
            <span
              aria-hidden="true"
              className="absolute inset-0"
              style={{
                backgroundColor: "var(--tenant-header-scrim-color)",
                opacity: "var(--tenant-header-scrim-opacity)",
              }}
            />
            <LogoBadge initials={resolvedTheme.logoInitials} />
            <span
              className="relative z-10 text-lg font-semibold"
              style={{ color: "var(--tenant-header-text-color)" }}
            >
              {resolvedTheme.consultantName}
            </span>
          </header>

          <div
            className="relative flex min-h-[220px] items-center px-8"
            style={{
              backgroundColor: "var(--tenant-bg-color)",
              backgroundImage: "var(--tenant-bg-image)",
              backgroundSize: "cover",
              backgroundPosition: "center",
            }}
          >
            <span
              aria-hidden="true"
              className="absolute inset-0"
              style={{
                backgroundColor: "var(--tenant-hero-scrim-color)",
                opacity: "var(--tenant-hero-scrim-opacity)",
              }}
            />
            <h2
              className="relative z-10 max-w-md text-4xl font-semibold"
              style={{ color: "var(--tenant-hero-text-color)" }}
            >
              Your next journey, curated by {resolvedTheme.consultantName}
            </h2>
          </div>

          {/* Fixed Layer 1 content living on a Layer 2 page — doc/DESIGN.md
              §3.1/§7: price/content cards never read tenant color. */}
          <div className="bg-neutral-50 px-8 py-6">
            <div className="max-w-sm rounded-md border border-neutral-200 bg-surface p-4">
              <div className="flex items-center justify-between">
                <span className="text-sm text-neutral-700">
                  7-night Goa &amp; Udaipur package
                </span>
                <Badge tone="success">Confirmed</Badge>
              </div>
              <p className="mt-2 text-2xl font-semibold text-neutral-900">
                ₹1,24,500
              </p>
              <p className="text-xs text-neutral-500">
                Fixed neutral text — never themed, regardless of preset.
              </p>
            </div>
          </div>
        </TenantThemedSurface>
      </div>
    </main>
  );
}

function LogoBadge({ initials }: { initials: string }) {
  return (
    <span
      className="relative z-10 flex h-10 w-10 items-center justify-center rounded-lg bg-surface text-sm font-semibold text-primary-600"
      style={{ padding: "8px" }}
    >
      {initials}
    </span>
  );
}

function ValidationReadout({
  label,
  result,
}: {
  label: string;
  result: {
    passes: boolean;
    ratioWithoutScrim: number;
    ratioWithScrim: number;
    scrimOpacity: number;
    textColorSource: "tenant" | "auto-fallback";
  };
}) {
  return (
    <div className="rounded-md bg-neutral-50 p-3 text-xs">
      <div className="flex items-center justify-between">
        <span className="font-medium text-neutral-700">{label}</span>
        <Badge tone={result.passes ? "success" : "error"}>
          {result.ratioWithScrim.toFixed(2)}:1
        </Badge>
      </div>
      <p className="mt-1 text-neutral-600">
        Raw pick: {result.ratioWithoutScrim.toFixed(2)}:1
        {result.scrimOpacity > 0 &&
          ` — scrim applied at ${Math.round(result.scrimOpacity * 100)}%`}
        {result.textColorSource === "auto-fallback" &&
          " — tenant color overridden with a safe fallback"}
      </p>
    </div>
  );
}
