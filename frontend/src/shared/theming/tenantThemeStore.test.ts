import { describe, expect, it } from "vitest";
import { useTenantThemeStore } from "./tenantThemeStore";
import { resolveTenantTheme } from "./resolveTenantTheme";

describe("tenantThemeStore", () => {
  it("starts with no active theme", () => {
    expect(useTenantThemeStore.getState().activeTheme).toBeNull();
  });

  it("sets and clears the active theme without a Provider", () => {
    const theme = resolveTenantTheme(
      {
        consultantName: "Test Co",
        logoUrl: null,
        backgroundImageUrl: null,
        backgroundColor: "#ffffff",
        textColorPrimary: "#1F2A44",
        textColorSecondary: "#1F2A44",
      },
      { header: "#ffffff", hero: "#ffffff" }
    );

    useTenantThemeStore.getState().setActiveTheme(theme);
    expect(useTenantThemeStore.getState().activeTheme).toEqual(theme);

    useTenantThemeStore.getState().setActiveTheme(null);
    expect(useTenantThemeStore.getState().activeTheme).toBeNull();
  });
});
