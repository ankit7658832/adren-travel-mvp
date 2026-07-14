/**
 * Layer 2 contrast-safety algorithm — doc/DESIGN.md §3.3.
 *
 * Pure functions only (no DOM/canvas access) so this module is trivially
 * unit-testable and reusable in both places doc/DESIGN.md requires it to
 * run identically: the Super Admin/Consultant live theme preview (§3.6)
 * and the runtime storefront theme resolution (§3.3 step 6).
 *
 * Image sampling (turning an uploaded background image into a flat
 * "effective background color" per zone) is a separate, DOM-dependent
 * concern — see sampleImageRegion.ts — and is intentionally not part of
 * this module.
 */

export interface RGB {
  r: number;
  g: number;
  b: number;
}

/** WCAG 2.1 AA threshold for normal-size text. doc/DESIGN.md §2, §3.3. */
export const AA_NORMAL_TEXT_RATIO = 4.5;

/** WCAG 2.1 AA threshold for large text / non-text UI components. */
export const AA_LARGE_TEXT_RATIO = 3;

/**
 * Ceiling on scrim opacity — doc/DESIGN.md §3.3 step 4. Past this point the
 * uploaded photo is no longer recognizably a photo, defeating the point of
 * letting the Consultant upload one.
 */
export const MAX_SCRIM_OPACITY = 0.55;

/** Step size for the opacity search — fine enough to be visually smooth. */
const SCRIM_OPACITY_STEP = 0.01;

const HEX_RE = /^#?([0-9a-fA-F]{6})$/;

export function hexToRgb(hex: string): RGB {
  const match = HEX_RE.exec(hex.trim());
  if (!match) {
    throw new Error(`Invalid hex color: "${hex}"`);
  }
  const int = parseInt(match[1], 16);
  return {
    r: (int >> 16) & 255,
    g: (int >> 8) & 255,
    b: int & 255,
  };
}

export function rgbToHex({ r, g, b }: RGB): string {
  const toHex = (c: number) =>
    Math.round(Math.min(255, Math.max(0, c)))
      .toString(16)
      .padStart(2, "0");
  return `#${toHex(r)}${toHex(g)}${toHex(b)}`;
}

function srgbChannelToLinear(c: number): number {
  const normalized = c / 255;
  return normalized <= 0.04045
    ? normalized / 12.92
    : Math.pow((normalized + 0.055) / 1.055, 2.4);
}

/** WCAG relative luminance, 0 (black) to 1 (white). */
export function relativeLuminance(color: RGB): number {
  const r = srgbChannelToLinear(color.r);
  const g = srgbChannelToLinear(color.g);
  const b = srgbChannelToLinear(color.b);
  return 0.2126 * r + 0.7152 * g + 0.0722 * b;
}

/** WCAG contrast ratio between two colors, 1:1 (no contrast) to 21:1 (max). */
export function contrastRatio(colorA: string, colorB: string): number {
  const lumA = relativeLuminance(hexToRgb(colorA));
  const lumB = relativeLuminance(hexToRgb(colorB));
  const lighter = Math.max(lumA, lumB);
  const darker = Math.min(lumA, lumB);
  return (lighter + 0.05) / (darker + 0.05);
}

/**
 * Composite a flat scrim color over a background at the given opacity, per
 * the standard CSS (non-premultiplied, sRGB-space) alpha-over formula —
 * this matches how a browser actually renders `background: scrimColor;
 * opacity: alpha` layered over the tenant background, so the computed
 * ratio matches what will actually be on screen.
 */
export function compositeOver(
  scrimColor: string,
  backgroundColor: string,
  opacity: number
): RGB {
  const fg = hexToRgb(scrimColor);
  const bg = hexToRgb(backgroundColor);
  const a = Math.min(1, Math.max(0, opacity));
  return {
    r: a * fg.r + (1 - a) * bg.r,
    g: a * fg.g + (1 - a) * bg.g,
    b: a * fg.b + (1 - a) * bg.b,
  };
}

export type TextColorSource = "tenant" | "auto-fallback";

export interface ResolveSafeTextColorParams {
  /** The Consultant-picked text color, e.g. `textColorPrimary`. */
  textColor: string;
  /**
   * The effective background: the flat `backgroundColor`, or the sampled
   * average color of the relevant zone for an uploaded image
   * (doc/DESIGN.md §3.3 step 1).
   */
  backgroundColor: string;
  /** Defaults to AA_NORMAL_TEXT_RATIO; pass AA_LARGE_TEXT_RATIO for large text zones (e.g. hero headline set at --text-4xl). */
  targetRatio?: number;
  /**
   * Fallback pair used when even a max-opacity scrim can't reach the
   * target — doc/DESIGN.md §3.3 fallback-chain step 1. Defaults to the
   * Layer 1 secondary/navy and white, per doc/DESIGN.md §2.1.
   */
  fallbackColors?: [string, string];
}

export interface ResolveSafeTextColorResult {
  /** Whether the target ratio is met by the final resolved rendering. */
  passes: boolean;
  /** Contrast ratio between the raw tenant text color and the raw background, no scrim. */
  ratioWithoutScrim: number;
  /** 0 if no scrim was needed; otherwise the minimum opacity found, capped at MAX_SCRIM_OPACITY. */
  scrimOpacity: number;
  /** null if no scrim needed; otherwise "#000000" or "#ffffff". */
  scrimColor: string | null;
  /** Contrast ratio actually achieved after applying the scrim (if any). */
  ratioWithScrim: number;
  /** "tenant" if the Consultant's own color is used (with or without a scrim); "auto-fallback" if it had to be overridden. */
  textColorSource: TextColorSource;
  /** The color that should actually be rendered as the text color. */
  resolvedTextColor: string;
}

/**
 * doc/DESIGN.md §3.3 — the contrast-safety algorithm.
 *
 * Given a tenant's picked text color and the effective background color of
 * the zone it will render in, returns everything needed to render it
 * safely: whether a scrim is required, at what opacity/color, and — only
 * if a max-opacity scrim still can't reach the target — a safe fallback
 * text color to use instead of the tenant's pick.
 */
export function resolveSafeTextColor({
  textColor,
  backgroundColor,
  targetRatio = AA_NORMAL_TEXT_RATIO,
  fallbackColors = ["#1f2a44", "#ffffff"],
}: ResolveSafeTextColorParams): ResolveSafeTextColorResult {
  const ratioWithoutScrim = contrastRatio(textColor, backgroundColor);

  if (ratioWithoutScrim >= targetRatio) {
    return {
      passes: true,
      ratioWithoutScrim,
      scrimOpacity: 0,
      scrimColor: null,
      ratioWithScrim: ratioWithoutScrim,
      textColorSource: "tenant",
      resolvedTextColor: textColor,
    };
  }

  // Scrim toward whichever pole (black or white) moves the background
  // luminance away from the text color's luminance — test both, keep
  // whichever reaches the target at the lower opacity (or gets closer).
  const candidates: Array<{ scrimColor: string }> = [
    { scrimColor: "#000000" },
    { scrimColor: "#ffffff" },
  ];

  let best: {
    scrimColor: string;
    opacity: number;
    ratio: number;
  } | null = null;

  for (const { scrimColor } of candidates) {
    for (
      let opacity = SCRIM_OPACITY_STEP;
      opacity <= MAX_SCRIM_OPACITY + 1e-9;
      opacity += SCRIM_OPACITY_STEP
    ) {
      const effectiveBg = rgbToHex(
        compositeOver(scrimColor, backgroundColor, opacity)
      );
      const ratio = contrastRatio(textColor, effectiveBg);

      if (ratio >= targetRatio) {
        if (!best || opacity < best.opacity) {
          best = { scrimColor, opacity, ratio };
        }
        break; // opacity is monotonically improving contrast in this direction; no need to search further
      }

      if (!best || ratio > best.ratio) {
        // Track the closest attempt in case neither direction reaches target.
        best = { scrimColor, opacity: MAX_SCRIM_OPACITY, ratio };
      }
    }
  }

  const scrimOpacity = best ? Math.min(best.opacity, MAX_SCRIM_OPACITY) : 0;
  const scrimColor = best ? best.scrimColor : null;
  const ratioWithScrim = best
    ? contrastRatio(
        textColor,
        rgbToHex(compositeOver(best.scrimColor, backgroundColor, scrimOpacity))
      )
    : ratioWithoutScrim;

  if (ratioWithScrim >= targetRatio) {
    return {
      passes: true,
      ratioWithoutScrim,
      scrimOpacity,
      scrimColor,
      ratioWithScrim,
      textColorSource: "tenant",
      resolvedTextColor: textColor,
    };
  }

  // Fallback chain (doc/DESIGN.md §3.3): even a max-opacity scrim can't
  // get the tenant's own text color to pass. Auto-substitute whichever
  // fallback color contrasts better against the scrimmed background.
  const effectiveBgAtMax = scrimColor
    ? rgbToHex(compositeOver(scrimColor, backgroundColor, scrimOpacity))
    : backgroundColor;

  const [fallbackA, fallbackB] = fallbackColors;
  const ratioA = contrastRatio(fallbackA, effectiveBgAtMax);
  const ratioB = contrastRatio(fallbackB, effectiveBgAtMax);
  const resolvedTextColor = ratioA >= ratioB ? fallbackA : fallbackB;
  const finalRatio = Math.max(ratioA, ratioB);

  return {
    passes: finalRatio >= targetRatio,
    ratioWithoutScrim,
    scrimOpacity,
    scrimColor,
    ratioWithScrim: finalRatio,
    textColorSource: "auto-fallback",
    resolvedTextColor,
  };
}
