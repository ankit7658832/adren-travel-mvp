import { describe, expect, it } from "vitest";
import {
  AA_NORMAL_TEXT_RATIO,
  MAX_SCRIM_OPACITY,
  compositeOver,
  contrastRatio,
  hexToRgb,
  relativeLuminance,
  resolveSafeTextColor,
  rgbToHex,
} from "./contrastSafety";

describe("hexToRgb / rgbToHex", () => {
  it("parses a hex color into RGB channels", () => {
    expect(hexToRgb("#5B2A9E")).toEqual({ r: 91, g: 42, b: 158 });
  });

  it("accepts hex without a leading #", () => {
    expect(hexToRgb("5B2A9E")).toEqual({ r: 91, g: 42, b: 158 });
  });

  it("round-trips through rgbToHex", () => {
    expect(rgbToHex(hexToRgb("#5b2a9e"))).toBe("#5b2a9e");
  });

  it("throws on an invalid hex color", () => {
    expect(() => hexToRgb("not-a-color")).toThrow();
    expect(() => hexToRgb("#fff")).toThrow(); // 3-digit shorthand not supported
  });
});

describe("relativeLuminance", () => {
  it("is 1 for white and 0 for black", () => {
    expect(relativeLuminance(hexToRgb("#ffffff"))).toBeCloseTo(1, 5);
    expect(relativeLuminance(hexToRgb("#000000"))).toBeCloseTo(0, 5);
  });
});

describe("contrastRatio", () => {
  it("is 21:1 for black on white (the WCAG maximum)", () => {
    expect(contrastRatio("#000000", "#ffffff")).toBeCloseTo(21, 1);
  });

  it("is 1:1 for identical colors", () => {
    expect(contrastRatio("#808080", "#808080")).toBeCloseTo(1, 5);
  });

  it("is symmetric regardless of argument order", () => {
    expect(contrastRatio("#5B2A9E", "#ffffff")).toBeCloseTo(
      contrastRatio("#ffffff", "#5B2A9E"),
      5
    );
  });

  it("matches the doc/DESIGN.md §2 documented ratios for Layer 1 tokens", () => {
    // These are the same numbers §2.1/§2.3 of doc/DESIGN.md cite as verified.
    expect(contrastRatio("#5B2A9E", "#ffffff")).toBeCloseTo(9.25, 1); // primary-600 on white
    expect(contrastRatio("#1F2A44", "#ffffff")).toBeCloseTo(14.26, 1); // secondary-900 on white
    expect(contrastRatio("#1E9E4A", "#ffffff")).toBeCloseTo(3.47, 1); // success-500 on white — fails AA normal text, by design
  });
});

describe("compositeOver", () => {
  it("returns the background unchanged at opacity 0", () => {
    expect(compositeOver("#000000", "#5B2A9E", 0)).toEqual({
      r: 91,
      g: 42,
      b: 158,
    });
  });

  it("returns the scrim color exactly at opacity 1", () => {
    expect(compositeOver("#000000", "#5B2A9E", 1)).toEqual({
      r: 0,
      g: 0,
      b: 0,
    });
  });
});

describe("resolveSafeTextColor — doc/DESIGN.md §3.3", () => {
  it("passes through the tenant's color untouched when it already meets AA", () => {
    const result = resolveSafeTextColor({
      textColor: "#1F2A44", // navy
      backgroundColor: "#ffffff",
    });

    expect(result.passes).toBe(true);
    expect(result.scrimOpacity).toBe(0);
    expect(result.scrimColor).toBeNull();
    expect(result.textColorSource).toBe("tenant");
    expect(result.resolvedTextColor).toBe("#1F2A44");
    expect(result.ratioWithoutScrim).toBeGreaterThanOrEqual(AA_NORMAL_TEXT_RATIO);
  });

  it("dark text on a dark background: fails outright, but a white scrim within the opacity cap fixes it", () => {
    const result = resolveSafeTextColor({
      textColor: "#1A1A1A",
      backgroundColor: "#000000",
    });

    expect(result.ratioWithoutScrim).toBeLessThan(AA_NORMAL_TEXT_RATIO);
    expect(result.passes).toBe(true);
    expect(result.textColorSource).toBe("tenant"); // scrim was enough, tenant's color is preserved
    expect(result.scrimColor).toBe("#ffffff"); // lightens the background away from the dark text
    expect(result.scrimOpacity).toBeGreaterThan(0);
    expect(result.scrimOpacity).toBeLessThanOrEqual(MAX_SCRIM_OPACITY);
    expect(result.ratioWithScrim).toBeGreaterThanOrEqual(AA_NORMAL_TEXT_RATIO);
  });

  it("light text on a near-identical light background: even a max-opacity scrim can't reach AA, so it falls back to a safe color", () => {
    const result = resolveSafeTextColor({
      textColor: "#F5F5F0",
      backgroundColor: "#FFFFFF",
    });

    expect(result.ratioWithoutScrim).toBeLessThan(AA_NORMAL_TEXT_RATIO);
    expect(result.textColorSource).toBe("auto-fallback");
    expect(result.scrimOpacity).toBe(MAX_SCRIM_OPACITY);
    expect(["#1f2a44", "#ffffff"]).toContain(result.resolvedTextColor);
    expect(result.resolvedTextColor).not.toBe("#F5F5F0"); // tenant's own pick was not usable
    expect(result.ratioWithScrim).toBeGreaterThanOrEqual(AA_NORMAL_TEXT_RATIO); // the fallback color itself must be safe
    expect(result.passes).toBe(true);
  });

  it("mid-tone case: gray text on an equally mid-tone gray background can't be rescued by any scrim direction, so it falls back", () => {
    const result = resolveSafeTextColor({
      textColor: "#808080",
      backgroundColor: "#808080",
    });

    expect(result.ratioWithoutScrim).toBeCloseTo(1, 1);
    expect(result.textColorSource).toBe("auto-fallback");
    expect(result.scrimOpacity).toBe(MAX_SCRIM_OPACITY);
    expect(result.resolvedTextColor).not.toBe("#808080");
    expect(result.ratioWithScrim).toBeGreaterThanOrEqual(AA_NORMAL_TEXT_RATIO);
    expect(result.passes).toBe(true);
  });

  it("respects a custom target ratio (e.g. AA-large for a big hero headline)", () => {
    const result = resolveSafeTextColor({
      textColor: "#1E9E4A", // success-500 — fails 4.5:1 normal text on white but passes 3:1 large text
      backgroundColor: "#ffffff",
      targetRatio: 3,
    });

    expect(result.passes).toBe(true);
    expect(result.scrimOpacity).toBe(0);
    expect(result.textColorSource).toBe("tenant");
  });

  it("uses custom fallback colors when provided", () => {
    const result = resolveSafeTextColor({
      textColor: "#808080",
      backgroundColor: "#808080",
      fallbackColors: ["#112233", "#eeeeee"],
    });

    expect(result.textColorSource).toBe("auto-fallback");
    expect(["#112233", "#eeeeee"]).toContain(result.resolvedTextColor);
  });
});
