import { describe, expect, it } from "vitest";
import { resolveTenantTheme, type BrandingProfile } from "./resolveTenantTheme";

const baseProfile: BrandingProfile = {
  consultantName: "Blue Horizon Travel",
  logoUrl: null,
  backgroundImageUrl: null,
  backgroundColor: "#ffffff",
  textColorPrimary: "#1F2A44",
  textColorSecondary: "#1F2A44",
};

describe("resolveTenantTheme", () => {
  it("derives initials from a two-word consultant name", () => {
    const theme = resolveTenantTheme(baseProfile, {
      header: "#ffffff",
      hero: "#ffffff",
    });
    expect(theme.logoInitials).toBe("BH");
  });

  it("derives initials from a single-word consultant name", () => {
    const theme = resolveTenantTheme(
      { ...baseProfile, consultantName: "Voyagr" },
      { header: "#ffffff", hero: "#ffffff" }
    );
    expect(theme.logoInitials).toBe("VO");
  });

  it("passes safe tenant colors through untouched", () => {
    const theme = resolveTenantTheme(baseProfile, {
      header: "#ffffff",
      hero: "#ffffff",
    });
    expect(theme.header.passes).toBe(true);
    expect(theme.header.textColorSource).toBe("tenant");
    expect(theme.hero.passes).toBe(true);
  });

  it("resolves an unsafe tenant text color against a sampled image zone color", () => {
    const theme = resolveTenantTheme(
      {
        ...baseProfile,
        backgroundImageUrl: "blob:sample-hero.jpg",
        textColorPrimary: "#F5F5F0", // near-white, unsafe on a light sampled zone
      },
      { header: "#FFFFFF", hero: "#FFFFFF" }
    );

    expect(theme.header.passes).toBe(true); // always resolves to *something* safe
    expect(theme.header.textColorSource).toBe("auto-fallback");
    expect(theme.header.resolvedTextColor).not.toBe("#F5F5F0");
  });
});
