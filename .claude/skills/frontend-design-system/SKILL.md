---
name: frontend-design-system
description: Layer 1 (Adren product chrome, fixed)/Layer 2 (white-label storefront, tenant-themed) design token architecture and the contrast-safety algorithm for ADREN's frontend. Use when writing or reviewing ANY styled UI — new component, restyled screen, or anything touching tenant branding — not just when reading doc/DESIGN.md for the first time.
metadata:
  type: project-skill
---

Full rationale, computed WCAG numbers, and per-screen breakdown: `doc/DESIGN.md`. This skill is the enforceable rule set distilled from it — read `doc/DESIGN.md` §3 before touching anything under `src/shared/theming/` or `src/features/consultant-storefront/` for the first time; this file is what to check against on every subsequent PR.

## The one rule everything else follows from

Two layers, architected separately, never allowed to leak into each other:

| | Layer 1 — Adren product chrome | Layer 2 — white-label surfaces |
|---|---|---|
| **Where** | `src/shared/design-system/`, every `src/features/` screen except the storefront | `src/shared/theming/`, `src/features/consultant-storefront/` |
| **Tokens** | `--color-*`, `--space-*`, `--text-*`, `--font-sans`, `--motion-*` (`src/shared/design-system/tokens.css`) — static, defined once at `:root` | `--tenant-*` (injected per-request by `TenantThemedSurface`, `src/shared/theming/TenantThemedSurface.tsx`) |
| **Who sets the values** | This design system | A Consultant, at onboarding — untrusted input |

**⚠️ Never let tenant input reach a Layer 1 surface.** No component outside `src/shared/theming/` or `src/features/consultant-storefront/` may read a `--tenant-*` CSS variable, ever — not "just this once for a quick demo," not for the Super Admin Console even though that's where branding gets *configured*. The console's own chrome stays Layer 1; only the live-preview panel inside it renders Layer 2 tokens, and it does so through `TenantThemedSurface`, not by hand.

**⚠️ Never let unvalidated tenant color reach a Layer 2 surface.** A raw `textColorPrimary`/`textColorSecondary` from a `BrandingProfile` must never be handed straight to a `style`/`className`. It always goes through `resolveSafeTextColor` (`src/shared/theming/contrastSafety.ts`) first — via `resolveTenantTheme.ts`, which is the only place that's allowed to call it in the render path. If you're writing a new Layer 2 component and reaching for `theme.textColorPrimary` directly instead of `theme.header.resolvedTextColor`/`theme.hero.resolvedTextColor`, stop — that's the exact bug class doc/DESIGN.md §3.3 exists to prevent.

## Fixed regardless of layer (doc/DESIGN.md §3.1, §4)

Never themeable, never tenant-colored, on any surface:
- Typography (`--font-sans` — Inter/Noto, self-hosted). Tenants get colors and imagery, never a font.
- Layout structure, spacing scale, component shapes/radii, motion timing.
- Semantic/status colors — success/warning/error/info (`src/shared/design-system/Badge.tsx`'s `tone` prop, never a raw color prop).
- Price displays, form validation, focus rings (`--color-focus-ring`, fixed `info-600` on both layers).
- Buttons/CTAs — always Layer 1 `primary` purple, even on the storefront. PRD §13.2 grants a Consultant a *text color*, not a brand/accent color (doc/DESIGN.md §3.1, flagged there as an interpretation needing sign-off — don't silently widen it).

## Building a new component

- **Status/badge anything** → use `Badge`'s `tone` enum (`neutral`/`success`/`warning`/`info`/`error`), never a raw hex or Tailwind color class. The raw brief colors fail AA as text-on-white in several cases (doc/DESIGN.md §2.2) — `Badge` already encodes the AA-safe fill+text pairing so you can't wire the failing one back in by accident.
- **New Layer 1 primitive** (button variant, form input, table) → Tailwind utility classes referencing the `primary`/`secondary`/`neutral`/etc. color scale in `tailwind.config.js`, which resolves through the CSS variables in `tokens.css`. Don't hardcode a hex value in a new component — if the token you need doesn't exist yet, add it to `tokens.css` + `tailwind.config.js` with a documented contrast ratio, matching doc/DESIGN.md §2's format, not as a one-off literal in the component.
- **New Layer 2 (storefront) content** → wrap it in `TenantThemedSurface`, read colors via `var(--tenant-header-text-color)` / `var(--tenant-hero-text-color)` (or add a new named `--tenant-*` zone through the same `resolveTenantTheme` → `TenantThemedSurface` pipeline if it's a genuinely new text-bearing zone — don't invent a third zone without also deciding what background gets sampled for it, per doc/DESIGN.md §3.3 step 1).
- **Supplier photos** (hotel/activity images) → always inside the fixed frame pattern (`aspect-ratio` + `object-fit: cover` + `neutral-200` border, doc/DESIGN.md §8), never a raw `<img>`, regardless of which layer/theme surrounds it.

## Contrast-safety algorithm — quick reference

`resolveSafeTextColor({ textColor, backgroundColor, targetRatio? })` in `contrastSafety.ts`, pure function, unit tested (`contrastSafety.test.ts`):

1. Tenant color already ≥ target ratio → passed through untouched (`textColorSource: "tenant"`, `scrimOpacity: 0`).
2. Fails → binary-scan a black-or-white scrim up to `MAX_SCRIM_OPACITY` (0.55); if a scrim gets it to target, tenant color is preserved, scrim applied (`textColorSource: "tenant"`, `scrimOpacity > 0`).
3. Even max scrim fails → auto-substitute the higher-contrast of the fallback pair (default navy/white), mark `textColorSource: "auto-fallback"`. The Super Admin picker UI (`ConsultantStorefront`'s left panel is the reference pattern) must block save and show why when this fires — don't silently accept the tenant's unusable pick.

**Known gap, not silently assumed fixed:** a background image with high internal contrast variance *within the sampled zone itself* can defeat a flat-average scrim for some pixels even when the aggregate math passes. Mitigation is the live preview rendering the real image (visual backstop), not a stronger guarantee — see doc/DESIGN.md §3.3's flagged limitation before treating any scrim result as an absolute guarantee.

## Checklist addendum (on top of `frontend-best-practices`)

- [ ] No component outside `src/shared/theming/`/`src/features/consultant-storefront/` reads a `--tenant-*` variable.
- [ ] No Layer 2 component reads a raw `BrandingProfile` color field directly — only `resolveTenantTheme`'s resolved output.
- [ ] New color usage is a token (`tokens.css` + `tailwind.config.js`) with a documented contrast ratio, not a literal hex.
- [ ] Status/state color uses `Badge`'s `tone` prop, not a raw color.
- [ ] Buttons/CTAs on any Layer 2 screen are still Layer 1 `primary`, not tenant-colored.
- [ ] New Layer 2 UI was checked against all three contrast-safety branches (pass / scrim-rescued / fallback-required) — the three `sampleBrandingPresets.ts` presets in `consultant-storefront` are the fastest way to exercise all three by hand.
