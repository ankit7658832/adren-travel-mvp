/**
 * doc/DESIGN.md §3.3, §6, §13 — the runtime Layer 2 theming mechanism.
 *
 * Scopes a ResolvedTenantTheme onto its subtree as CSS custom properties,
 * computed per-request from data (not baked in at build time —
 * doc/DESIGN.md §6). Only components under
 * src/features/consultant-storefront/ and this directory may read
 * --tenant-* variables; Layer 1 components never do
 * (.claude/skills/frontend-design-system/SKILL.md).
 *
 * Which Consultant's theme is "active" app-wide lives in
 * tenantThemeStore.ts (Zustand, per doc/architecture/RULES.md §7.1) — this
 * component just renders a given ResolvedTenantTheme, it doesn't look one
 * up itself.
 */
import type { CSSProperties, ReactNode } from "react";
import type { ResolvedTenantTheme } from "./resolveTenantTheme";

interface TenantCssVars extends CSSProperties {
  [key: `--${string}`]: string;
}

export function TenantThemedSurface({
  theme,
  children,
  className,
}: {
  theme: ResolvedTenantTheme;
  children: ReactNode;
  className?: string;
}) {
  const style: TenantCssVars = {
    "--tenant-bg-color": theme.backgroundColor,
    "--tenant-bg-image": theme.backgroundImageUrl
      ? `url(${theme.backgroundImageUrl})`
      : "none",
    "--tenant-logo-url": theme.logoUrl ? `url(${theme.logoUrl})` : "none",
    "--tenant-header-text-color": theme.header.resolvedTextColor,
    "--tenant-header-scrim-color": theme.header.scrimColor ?? "transparent",
    "--tenant-header-scrim-opacity": String(theme.header.scrimOpacity),
    "--tenant-hero-text-color": theme.hero.resolvedTextColor,
    "--tenant-hero-scrim-color": theme.hero.scrimColor ?? "transparent",
    "--tenant-hero-scrim-opacity": String(theme.hero.scrimOpacity),
  };

  return (
    <div data-tenant-theme className={className} style={style}>
      {children}
    </div>
  );
}
