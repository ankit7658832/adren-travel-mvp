# ADREN TRAVEL — Design System

**Owner:** Design/Frontend
**Status:** Draft for Review — see [§12 Open Decisions](#12-open-decisions-requiring-sign-off) before treating this as final.
**Companion PRD sections:** Part 3 (Personas), §13.2 (Branding Configuration), §17 (Regional Compliance & Localization), Part 21 (Screen-by-Screen UI Spec).
**Companion code:** `frontend/src/shared/design-system/` (Layer 1 tokens + Tailwind config), `frontend/src/shared/theming/` (Layer 2 runtime injection + contrast-safety utility).

---

## 0. The one decision everything else follows from

PRD §13.2 lets every Consultant upload a logo, upload a background image, and pick their own text color at onboarding. Nothing else about the visual system is tenant-configurable — not layout, not spacing, not iconography, not the button color, not the font. That is a narrow grant, and it is the right one: a Consultant is a travel agent, not a designer, and the four fields they're given (logo, background, two text colors, domain) are exactly the fields that carry *brand identity* without touching *usability*.

This document is therefore built around two layers that are architected separately from line one, not bolted together after the fact:

| | Layer 1 — Adren product chrome | Layer 2 — White-label surfaces |
|---|---|---|
| **Who sees it** | Super Admin, Consultant, Consultant's Users (internal staff) | End Traveler, via the Consultant's domain |
| **Screens** | Search Dashboard, Itinerary Builder, Package Builder, Consultant Dashboard, Super Admin Console, Wallet & Billing, Campaign Builder, PNR Search, Notification Preferences — 9 of PRD Part 21's 10 screens | Consultant storefront, quotations, vouchers; the themed header of the Booking & Payment flow (21.4) |
| **Who controls the visuals** | Adren design system. Fixed. | The Consultant, within 4 tenant tokens: logo, background image, primary text color, secondary text color |
| **Trust level of the input** | Designed and reviewed by Adren | Uploaded by a non-designer; must be assumed illegible, off-brand, or inaccessible until proven otherwise |

Every section below is written against this split. Section 3 is the core deliverable — the contrast-safety algorithm — because it's the mechanism that makes Layer 2 safe to ship at all.

---

## 1. Brand rationale (Layer 1)

Starting palette (no logo file exists in the repo — `frontend/public/` and `doc/brand/` were checked and are empty of brand assets — so this document defines the palette from the brief and it becomes canonical pending a real logo file):

- Purple `#5B2A9E`, Blue `#2E6FE0`, Green `#1E9E4A`, Gold `#F2B705`, Red `#E2231A`, Navy `#1F2A44`

**Mapping to Layer 1 roles:**

| Color | Role | Why |
|---|---|---|
| **Purple** | **Primary** — CTAs, active nav state, links, focus accents | Every other color in the set already carries a strong, near-universal UI convention (red=danger, green=success, blue=info, gold=caution). Purple is the one hue with no competing semantic pull, which makes it available as the single "this is an action" color. It's also the least-used primary in B2B SaaS (blue is oversaturated — Salesforce, most fintech, most travel-tech competitors named in PRD §1: TBO, Travelomatix-class tools), so it buys ADREN visual distinctiveness in a crowded category without inventing an off-brand color. |
| **Navy** | **Secondary** — headings, sidebar/nav chrome, high-emphasis text | Highest-contrast neutral-adjacent color in the set (14.26:1 on white, see §2). Doubles as the "structural ink" color so body chrome doesn't compete with the purple primary for attention. |
| **Blue** | **Info** | Matches existing convention (info banners, links-that-aren't-CTAs, PNR/reference badges). Reusing convention over inventing a new mapping reduces cognitive load in a *dense* B2B tool where recognition speed matters more than novelty. |
| **Green** | **Success** | Convention: confirmed bookings, published packages, "Live" campaign status. |
| **Gold** | **Warning** | Convention: pending states (Pending Approval, Pending Policy Review, credit-limit-approaching). Also the only color in the set that reads correctly as "caution" rather than "stop" or "go." |
| **Red** | **Error/Danger** | Convention: failed supplier search, validation errors, rejected campaigns, credit-limit breach. |

This gives Layer 1 **one** action color (purple) and five status/accent colors, each justified by an existing convention rather than a novel one — deliberate, because a dense B2B tool full of status enums (Booking, Quotation, Ad Campaign, Local DMC vetting — PRD §20) is exactly where color-convention violations cost the most user error.

Layer 1 is fixed. It is never overridden by tenant input, and it is not customer-facing beyond the Consultant's/User's own internal use of the product (Search Dashboard, Itinerary Builder, etc.).

---

## 2. Color tokens (Layer 1)

All ratios below are computed with the WCAG 2.1 relative-luminance formula (sRGB → linear → `0.2126R + 0.7152G + 0.0722B`), not estimated. Script used to verify is reproduced in `frontend/src/shared/design-system/contrast.test.ts` so these numbers stay checked against the actual token file, not just this document.

### 2.1 Primary / Secondary

| Token | Hex | Used for | On | Ratio | AA normal text (4.5:1) | AA large text/UI (3:1) |
|---|---|---|---|---|---|---|
| `--color-primary-600` | `#5B2A9E` | Buttons, links, active states | white | **9.25:1** | ✅ | ✅ |
| `--color-primary-700` | `#4A2180` | Hover/active button state | white | **11.54:1** | ✅ | ✅ |
| `--color-primary-50` | `#F3EDFB` | Selected-row tint, subtle highlight | — | — | — | — |
| `--color-secondary-900` | `#1F2A44` (navy) | Headings, nav/sidebar bg, high-emphasis text | white | **14.26:1** | ✅ | ✅ |
| `--color-secondary-700` | `#334063` | Secondary nav text, muted headings | white | 9.5:1 (computed, same family) | ✅ | ✅ |

### 2.2 Status colors — text-safe variants introduced deliberately

The raw brief colors, used naively as text-on-white, **fail AA** in three of four cases. Rather than quietly picking a "close enough" shade, every failure is stated here and a passing variant is introduced:

| Pairing tested | Ratio | Verdict |
|---|---|---|
| `green-600 #1E9E4A` text on white | 3.47:1 | **FAILS AA normal text.** Passes AA-large (≥3:1) only. |
| `gold-500 #F2B705` text on white | 1.82:1 | **FAILS badly** — not usable as text on white at any size. |
| `gold-600 #B8860B` text on white | 3.25:1 | **FAILS AA normal text.** |
| `blue-600 #2E6FE0` text on its own `blue-50` tint | 4.14:1 | **FAILS AA normal text** (passes fine on plain white, 4.70:1). |
| `red-600 #E2231A` text on its own `red-50` tint | 4.04:1 | **FAILS AA normal text** (passes on plain white, 4.68:1, but only barely). |

Rejected pairings are never wired into the token file as text colors. Resolution: a `-500` (or brief-original) shade is kept for **large text, icons, and fills only**; a separate, darker `-700` shade is introduced wherever the color needs to render as **body-size text**, including on its own tint background.

| Token | Hex | Role | Ratio (on stated bg) | Verdict |
|---|---|---|---|---|
| `--color-success-500` | `#1E9E4A` | Icon fill, large text (≥18px/24px bold), status dot | 3.47:1 on white | AA-large only |
| `--color-success-700` | `#167C3D` | Success body text, badge label text | 5.27:1 on white | ✅ AA normal |
| `--color-success-50` | `#E9F8EE` | Success badge/toast background | — | — |
| `--color-warning-500` | `#F2B705` (brief gold) | Badge/chip **fill**, icon — never text-on-white | 7.84:1 with navy-900 text on top | ✅ (as a fill under navy text, see below) |
| `--color-warning-700` | `#8A6100` | Warning body text | 5.54:1 on white, 5.13:1 on `warning-50` | ✅ AA normal |
| `--color-warning-50` | `#FEF6DC` | Warning badge/toast background | — | — |
| `--color-info-600` | `#2E6FE0` | Links, info icon, large text, badge fill | 4.70:1 on white | ✅ AA normal (plain white only) |
| `--color-info-700` | `#1F52AC` | Info text on `info-50` tint | 6.47:1 on `info-50` | ✅ AA normal |
| `--color-info-50` | `#EAF1FD` | Info badge/toast background | — | — |
| `--color-error-600` | `#E2231A` | Error text/icon on plain white | 4.68:1 on white | ✅ AA normal (barely — treat as floor, don't lighten further) |
| `--color-error-700` | `#B01912` | Error text on `error-50` tint | 6.05:1 on `error-50` | ✅ AA normal |
| `--color-error-50` | `#FDEAE9` | Error/validation background | — | — |

**Design rule this produces:** status badges render as *fill + dark text* (e.g. `warning-500` fill with `secondary-900` navy text, 7.84:1) or *tint + darkened text* (e.g. `warning-50` fill with `warning-700` text, 5.13:1) — never brief-original color as text-on-white. This rule is enforced by the Badge component (§7) taking a `status` enum prop, not a raw color prop, so engineers can't accidentally wire the failing pairing back in.

### 2.3 Neutrals & surfaces

| Token | Hex | Role | Ratio on white | Verdict |
|---|---|---|---|---|
| `--color-neutral-900` | `#1A1D26` | Primary body text | 16.83:1 | ✅ |
| `--color-neutral-700` | `#3D4354` | Secondary text | 9.86:1 | ✅ |
| `--color-neutral-600` | `#525A6B` | Muted text, placeholders (≥ body size) | 6.92:1 | ✅ |
| `--color-neutral-500` | `#6B7280` | Disabled text (still must be legible per WCAG — not decorative) | 4.83:1 | ✅ AA normal |
| `--color-neutral-400` | `#8B93A3` | Borders/icons on interactive controls (non-text) | 3.09:1 | AA-large/UI-component only (WCAG 1.4.11) — **not for text** |
| `--color-neutral-300` | `#C7CCD6` | Dividers, disabled control borders | 1.61:1 | **Fails even 3:1** — used only for purely decorative elements exempt under WCAG 1.4.11 (never a focus indicator, never a required-to-perceive boundary) |
| `--color-neutral-200` | `#E3E7ED` | Card borders, table row dividers | — | decorative, same exemption |
| `--color-neutral-100` | `#F1F3F6` | Subtle surface tint (hover row) | — | — |
| `--color-neutral-50` | `#F8F9FB` | App background | — | — |
| `--color-surface` | `#FFFFFF` | Card/panel background | — | — |

The original brief's implied gray (`#9AA3B2`, an earlier draft of `neutral-400`) was tested at 2.54:1 on white and **rejected outright** — it fails even the relaxed 3:1 non-text threshold. `#8B93A3` replaces it.

### 2.4 Dark mode

**Out of scope for MVP.** This is a dense B2B operations tool (search → itinerary → booking, PRD §9.1) used during business hours at a desk; nothing in PRD Part 21 or the personas asks for it, and a second maintained palette (all of §2.1–2.3 doubled, re-verified for contrast) is real ongoing cost for a team also building supplier integrations, AI governance, and white-label theming in the same MVP window. Flagged in §12 as a scope call, not a silent omission.

---

## 3. White-label theming architecture (Layer 2) — core deliverable

### 3.1 The exact tenant-overridable set

Per PRD §13.2, literally four inputs, no more:

```
tenant.logoUrl
tenant.backgroundImageUrl   // OR tenant.backgroundColor if no image uploaded
tenant.textColorPrimary
tenant.textColorSecondary
```

(`tenant.domain` / CNAME mapping is also part of §13.2 but is a routing concern, not a visual token — out of scope for this document.)

**Everything else is fixed, even on Layer 2 surfaces:**

- Layout structure (grid, column counts, header height, card structure)
- Spacing scale (§5)
- Component shapes — border-radius, shadow depth, button/input geometry
- Iconography (§8) — always Lucide, always Adren's stroke width, never tenant icons
- Typography (§4) — always Inter/Noto, tenant cannot supply a font
- All semantic/status colors (§2.2) — success/warning/error/info never shift with tenant theme
- All price displays — always `neutral-900` on `surface`, regardless of tenant color, so a price is never rendered in a tenant's (potentially low-contrast, potentially garish) text color
- Focus rings, form validation styling
- CTA/button fill color — **buttons stay Adren primary-purple even on the storefront.** PRD §13.2 grants *text color*, not a brand/accent color. This is a deliberate reading, not an oversight — flagged in §12 for explicit sign-off since it constrains what "customization" means to the Consultant.

This is the whole point of the split: a Consultant fiddling with two text colors and a background image cannot, structurally, break a form, mis-render a price, or make an error message illegible — those surfaces don't read tenant tokens at all.

### 3.2 The contrast-safety problem, precisely stated

The dangerous pairing is: **arbitrary Consultant-picked text color** rendered **directly over arbitrary Consultant-uploaded imagery or flat color**, with no professional design review in between. This only actually happens in two places on a themed page: the **storefront header/nav band** (logo + Consultant name/tagline) and the **hero banner** (headline text over the background image). Every other piece of text on a Layer 2 page — search results, quotation line items, prices, forms — sits on a fixed white/neutral Adren card surface layered *on top of* the tenant background, so it never touches tenant color and is never at risk (that's §3.1's fixed-elements list doing the work). This scoping matters: it turns "guarantee contrast for text anywhere on an arbitrary page" (intractable) into "guarantee contrast for text in two fixed, known zones" (tractable).

### 3.3 Primary strategy: scrim/overlay, computed and cached at save time

**Chosen strategy: (a) auto-inserted semi-transparent scrim**, not (b) silent fallback-color substitution and not (c) hard block-on-save as the *primary* mechanism. Reasoning:

- A scrim is the only one of the three that gives a **mathematical, image-content-independent guarantee**. Given a background luminance and a target contrast ratio, the required scrim opacity is a closed-form calculation — it doesn't matter what's in the photo. Fallback-color substitution (b) fixes the text but silently discards the Consultant's choice, which is the one thing PRD §13.2 explicitly grants them; that's a worse product experience than dimming the photo slightly. A hard block (c) on its own leaves the Consultant stuck with no visible reason if they don't understand contrast math.
- A scrim also **preserves the tenant's actual uploaded asset** (the background image is usually the thing they care about most — their venue photos, their brand imagery) while only ever adjusting a translucent tint over the small zone where text sits, not the whole image.

**Algorithm (implemented in `frontend/src/shared/theming/contrastSafety.ts`):**

1. **At branding-save time** (Super Admin/Consultant picker, FND-06's flow), sample the **effective background color** of the two text-bearing zones only:
   - Flat `backgroundColor` → exact value, no sampling needed.
   - Uploaded image → downsample the image to a small canvas (e.g. 32×32) restricted to the *header band* (top ~15% of the image, where the nav/logo sits) and separately the *hero-text safe zone* (the area the headline is laid out into, per §3.4's safe-zone definition), and take the average RGB of each region. This is a single one-time computation at upload/save, not a per-request cost.
2. Compute WCAG contrast ratio between the Consultant's picked text color and each sampled background region using the same relative-luminance formula as §2.
3. **If ratio ≥ 4.5:1** (normal text) — no scrim needed for that zone. Store `scrimOpacity: 0`.
4. **If ratio < 4.5:1** — binary-search the minimum black-or-white scrim opacity (scrim color chosen by whichever pole — `#000` or `#FFF` — is on the *same side* of the background's luminance the text needs to move away from) that brings the ratio to ≥4.5:1, capped at **0.55 max opacity** (past that, the photo is no longer recognizably a photo, which defeats the purpose of letting the Consultant upload one). Store the resolved `scrimOpacity` and `scrimColor`.
5. **Resolved tokens, not raw tenant input, are what components consume.** The output of this pipeline — `textColorPrimaryResolved`, `scrimOpacity`, `scrimColor` — is written back onto the `BrandingProfile` record (extends FND-06's entity) alongside the raw tenant input, and it's these resolved fields that FND-07's cache/propagation mechanism ships to the live storefront. Runtime page loads apply cached CSS variables; they never re-run image analysis on every request.
6. **Client-side, defense in depth:** the same pure function runs again at render time in the storefront's theme provider as a cheap re-check (using the cached resolved values, not re-sampling the image) — if a resolved value is ever missing (e.g., older BrandingProfile row from before this pipeline existed), the component falls back to computing against the flat `backgroundColor` field synchronously rather than rendering unchecked tenant color.

**Fallback chain, when the primary strategy still can't guarantee legibility:**

1. Scrim at max opacity (0.55) still < 4.5:1 → **auto-substitute the text color** with whichever of `secondary-900` (navy) or white has higher contrast against the *scrimmed* background, and mark the resolved record `textColorSource: "auto-fallback"` (not `"tenant"`) so the Super Admin UI can visibly show "we overrode your color here" rather than silently diverging from what the Consultant thinks they picked.
2. Independently of (1) actually resolving the render, **the Super Admin/Consultant picker UI is blocked from saving** with the tenant's chosen color until they either accept the auto-fallback, pick a different text color, or re-upload a lower-variance image — surfaced via the live preview component (§3.6). This is strategy (c), used as an upstream correction mechanism, not the runtime safety net — the runtime always renders something legible (via 1 and the scrim), but save-time friction pushes Consultants toward fixing the actual asset rather than accumulating auto-overridden colors silently forever.

**This is the one place in this document flagged as not 100%-guaranteed by math for every possible upload:** a background image with **high internal contrast variance inside the sampled zone itself** (e.g., a photo that's half black rooftop and half white sky, both within the header band) has an *average* color that may sit in a safe mid-range while individual pixels behind individual letters are not — a flat scrim computed from the region average cannot fix this for 100% of pixels in 100% of images, because CSS cannot vary scrim opacity per-pixel without expensive real-time image segmentation, which is out of scope for MVP. **Practical mitigation:** (a) the scrim, even sized off the average, substantially narrows the achieved-vs-required gap for the common case; (b) the fallback-chain's step 1 (auto text-color substitution against the *scrimmed* average) still fires and generally reads correctly in practice even under moderate variance; (c) the live preview (§3.6) renders the *actual* image with the *actual* computed scrim before save, so a Super Admin looking at a genuinely bad case (checkerboard-style extreme variance) sees it visually and can reject the image, which is the real backstop for the residual risk. This limitation and mitigation should be stated to Consultants in the branding-config UI copy, not just this document.

### 3.4 Background-image handling

- **Object-fit:** `cover`, anchored via `object-position: center top` for the header band, `center` for the hero — chosen so the header/logo zone (top) is protected from cropping first, since that's where static text always renders; a wide/short crop on ultra-wide monitors loses hero-image sides before it loses the header band.
- **Safe zone:** headline text is laid out inside a max-width column that never exceeds the horizontal safe area (10% margin) that survives `cover`-cropping at the narrowest supported breakpoint (mobile, §5), so the same image doesn't need per-breakpoint art direction.
- **Fallback when no image uploaded, or image fails to load** (`onerror`): Adren default — a fixed, low-contrast-with-nothing diagonal gradient between `secondary-900` and `primary-700` (both Layer 1 tokens, never tenant-influenced), so an un-onboarded or broken-image Consultant's storefront still looks intentional rather than broken.
- **Upload guidance surfaced in the picker UI:** recommend ≥1920×600px for the hero/header band, JPEG/WebP, **max 5MB**; a smaller/lower-res image is accepted but upscaled with a visible picker-UI warning ("may look soft on large screens") rather than rejected outright — MVP doesn't block on this, only warns.

### 3.5 Logo handling

- **Fixed container**, not a fixed image size: `max-height: 40px` in the storefront nav bar, `max-height: 64px` in the hero if used there; `max-width: 200px` in both; `padding: 8px`; `object-fit: contain` — this absorbs any aspect ratio or resolution the Consultant uploads without ever stretching it or breaking the header's fixed height (which is a Layer 1 layout constant — §3.1).
- **Transparent-PNG-safe:** the logo container itself renders as a small `neutral-50`/white rounded chip (`border-radius: 8px`) sitting on top of the tenant background/scrim, *not* directly on the raw background — this guarantees a transparent-background logo with dark strokes stays legible regardless of what's behind it, without needing the contrast-safety pipeline to reason about the logo image's internal content at all (it sidesteps the problem rather than solving a harder version of §3.3 for arbitrary logo art).
- **Placeholder when no logo uploaded yet:** Consultant's initials (first letters of business name, max 2 chars) in an Adren-styled circular badge — `primary-600` fill, white text — i.e., the placeholder is explicitly Layer 1 styled, not tenant-colored, since no validated tenant color exists to use safely before onboarding completes.

### 3.6 Which elements read tenant color, and the live preview/validation component

**Allowed to use `textColorPrimaryResolved`/`textColorSecondaryResolved`:** storefront nav wordmark/tagline text, hero headline and subheadline, section headers on marketing-style storefront content blocks.

**Always fixed regardless of tenant input** (restated from §3.1 for this specific list): error/validation messages, price displays, status badges, form labels and inputs, buttons/CTAs, focus indicators. No component in `frontend/src/shared/design-system/` (Layer 1 primitives) ever reads a `--tenant-*` CSS variable — this is enforced by convention + the SKILL.md rule (see companion skill file), and only the small set of Layer-2-specific components in `frontend/src/shared/theming/` are allowed to.

**Live preview + validation component** (`ThemePreview`, for the Super Admin/Consultant branding-config screen, PRD §13.2 flow / stories FND-06, FND-07):

- Renders a **real sample page**, not swatches: an actual storefront header (logo container + wordmark) and hero block (background image/color + headline), built from the *same* components the live storefront uses, driven by the in-progress (unsaved) picker state.
- Runs the §3.3 contrast pipeline live, client-side, on every color/image change (debounced against the image-sampling step) and shows an inline result next to each text field: `"6.8:1 — passes AA"` or `"3.1:1 — fails AA; a 42% scrim will be applied automatically"` or, at the fallback-chain edge, `"Even with a scrim, your text color isn't safe here — we'll use navy instead unless you pick a different color or image."`
- **Save is blocked** (button disabled, not just warned) only in the fallback-chain condition (§3.3, chain step 2) — i.e., friction is proportional to severity: a scrim-correctable case saves freely (with the scrim visibly shown in the same preview so nothing is a surprise later), a fallback-triggering case requires acknowledgement.

---

## 4. Typography — Layer 1, fixed everywhere (including Layer 2)

**Font: Inter**, self-hosted (not loaded from Google Fonts' CDN). Justification for a dense B2B tool:

- Tall x-height and a genuinely tabular numeral set (`font-variant-numeric: tabular-nums`) — this product is full of dense price/markup tables (PRD §12 worked examples, Wallet ledger §20.12) where digits need to align in columns; most display-oriented fonts don't tune for this, Inter does.
- Self-hosting (rather than a Google Fonts `<link>`) avoids a third-party-CDN GDPR consent question in the UK/Denmark markets (PRD §17.1 — UK GDPR, EU GDPR) — a font request to a third-party domain on every page load is exactly the kind of thing EU privacy guidance has flagged in the past; self-hosting sidesteps the question entirely rather than requiring a legal read on it.
- Free, open license (SIL OFL), no runtime dependency risk.

**Fallback stack** (multi-script, for target markets India/Australia/UK/USA/Dubai/Denmark — PRD §13.3, §17):
```
font-family: "Inter", "Noto Sans", "Noto Sans Devanagari", system-ui, -apple-system, sans-serif;
```
`Noto Sans` covers Danish (æ/ø/å) and broad Latin Extended; `Noto Sans Devanagari` is included per PRD §13.3's "Hindi/regional consideration" for India, self-hosted alongside Inter so Hindi-labeled content (if/when MVP scope includes it — see §12) doesn't silently fall back to a mismatched system font.

Tenants **cannot** override the font, on Layer 2 or anywhere else — this is explicit, not an oversight: letting a Consultant pick a display font is a much larger legibility/performance risk than a text color, and PRD §13.2 doesn't grant it.

**Type scale** (px, 1.25-ish ratio tuned for a dense UI over a dramatic one):

| Token | Size / line-height | Use |
|---|---|---|
| `--text-xs` | 12px / 16px | Table meta, badges, timestamps |
| `--text-sm` | 14px / 20px | Body default in dense tables/forms |
| `--text-base` | 16px / 24px | Default body text |
| `--text-lg` | 18px / 28px | Card titles, emphasized body |
| `--text-xl` | 20px / 28px | Section headers |
| `--text-2xl` | 24px / 32px | Panel/page titles |
| `--text-3xl` | 30px / 36px | Dashboard summary numbers |
| `--text-4xl` | 36px / 44px | Storefront hero headline (Layer 2, still this fixed scale) |

---

## 5. Spacing & layout grid

**Spacing scale**, 4px base unit: `0, 4, 8, 12, 16, 20, 24, 32, 40, 48, 64, 80` (`--space-1` … `--space-20` in 4px steps) — standard, keeps every margin/padding decision on-grid rather than ad hoc.

**Breakpoints** (desktop-first — this is an at-a-desk operations tool per PRD §9.1's "median search-to-itinerary ≤10 minutes" target, not a mobile-first consumer app):

| Token | Width | Behavior |
|---|---|---|
| `2xl` | ≥1536px | Full dashboard, map + itinerary panels side by side (21.1/21.2) |
| `xl` | ≥1280px | Design target — primary desktop layout |
| `lg` | ≥1024px | Sidebar remains, panels stay side-by-side but tighter |
| `md` | ≥768px | Sidebar collapses to icon rail; map/itinerary panels **stack** (map on top) per PRD 21.1's explicit "Map panel (left/top on mobile)" note |
| `sm` | <768px | Single column, sidebar becomes a drawer, tables switch to stacked-card view |

Degradation target is "reasonable," not "equally optimized" — PRD Part 21 doesn't ask for a mobile-first rebuild, and Consultants/Users are the primary audience for the layer where this matters (Layer 1).

---

## 6. Component library decision

**Recommendation: Tailwind CSS + hand-built primitives following shadcn/ui's conventions (CSS-variable-driven, Radix-based where interaction complexity warrants it — e.g. Dialog, Select), not a full shadcn CLI install for MVP.**

Why CSS-variable theming specifically (this is the load-bearing reason, not a stylistic preference): **Layer 2 tokens must be swappable per-request/per-domain at runtime.** A compile-time-only system — Sass variables, CSS-in-JS that resolves at build time, Tailwind's own color palette used directly without a variable indirection — bakes colors into the shipped CSS/JS bundle at build time. That's fine for Layer 1 (one build, one set of colors, ships once). It's unworkable for Layer 2: there could be hundreds of Consultants, each with their own colors, and none of them are known at build time — they're rows in `BrandingProfile`, resolved per-domain at request time (that's the entire point of FND-07's "no redeploy" requirement). CSS custom properties solve this natively: `bg-[var(--tenant-bg-color)]` in Tailwind (or `background: var(--tenant-bg-color)` in a component) reads whatever value is currently set on an ancestor element — which the theming provider (§ implementation, `TenantThemeProvider`) sets by writing `style` attributes / a `<style>` block scoped to a `[data-tenant-theme]` wrapper, computed from the resolved `BrandingProfile` fetched for the current domain. No rebuild, no per-tenant bundle, one shipped app.

Why Tailwind specifically for the utility layer: fast to write consistent spacing/sizing without inventing new CSS per component (matters for velocity on a small team also building supplier integrations, AI governance, and payments in the same MVP), and its arbitrary-value syntax (`var(--x)`) composes cleanly with the CSS-variable approach above rather than fighting it.

Why hand-built primitives over the shadcn CLI for MVP, specifically: the CLI vendors a fixed component set into the repo from a registry at generation time — useful once the team wants the full breadth (command palette, combobox, etc.), but for the handful of primitives this MVP actually needs right now (Button, Input, Card, Badge, Dialog) it's lower-risk to hand-write them directly against the same conventions (CSS variables named `--background`/`--foreground`/`--primary` etc., `class-variance-authority` for variants, `clsx`/`tailwind-merge` for composition) so a real `shadcn add <component>` later drops in without a token-naming migration.

---

## 7. Core component specs

All specs below are Layer 1 by default; each entry states explicitly if/how it appears on Layer 2.

**Button** — variants `primary` (purple-600 fill, white text), `secondary` (white fill, neutral-300 border, neutral-900 text), `ghost` (transparent, neutral-700 text), `destructive` (error-600 fill, white text). States: default/hover(`-700` shade)/active/focus(2px `info-600` ring, offset 2px, **fixed color, never tenant**)/disabled(`neutral-200` fill, `neutral-400` text)/loading(spinner replaces label, width locked to prevent layout shift). Sizes: `sm` 32px / `md` 40px / `lg` 48px height. **Appears on Layer 2** (storefront CTAs) but always as `primary` variant with Adren purple — never tenant-colored (§3.1).

**Form inputs** — text/select/date-range/multiselect-autocomplete (the Search Dashboard's location box, §10 below, is the reference instance). Default border `neutral-300`, focus border `primary-600` + ring, error state always `error-600` border + `error-50` background + `error-700` inline message — **fixed regardless of layer**, per §3.1's explicit carve-out for validation. **Layer 1 only** — the traveler-facing side of Booking & Payment (21.4) uses these same fixed styles even though it sits inside a Layer 2-themed page (mixed screen, see §10).

**Tables/data grids** — dense row height 40px, sticky header, `neutral-200` row dividers (decorative, exempt per §2.3), hover row = `neutral-50`, right-aligned numeric columns with `tabular-nums`. Used for PNR Search (21.9), Wallet ledger (21.7), Super Admin credential/audit lists (21.6). **Layer 1 only.**

**Cards** — `surface` (white) background, `neutral-200` 1px border, `8px` radius, `--space-4`/`--space-6` internal padding. Search-result cards, package cards. On Layer 2 (quotation/voucher line items), cards stay this same fixed white/neutral styling regardless of tenant theme — **only the page chrome around the cards is themed**, never the card content itself, which is exactly what keeps prices/line-items legible independent of tenant choices.

**Modals** (Radix Dialog primitive) — focus-trapped, `surface` background, `neutral-900/40` scrim behind (this is a Layer 1 UI scrim, unrelated to the §3.3 tenant-background contrast scrim — different purpose, don't conflate). **Always Layer 1 styled**, even when triggered from a Layer 2 page (e.g. a "Confirm Booking" modal inside the traveler-facing flow is Adren chrome, not tenant-themed) — this is the concrete instance of the "mixed" screen category in §10.

**Status/badge indicators** — pill shape, `12px` text, mapped to PRD entity status enums:
- Quotation: draft/sent/accepted/expired
- Booking: pending/confirmed/cancelled
- Ad Campaign (§20.13): Pending Approval → Pending Policy Review → Live / Rejected (rendered as a stepper, not just a badge, per 21.8)
- Local DMC vetting: unvetted/pending/approved/rejected

Each status maps to one of the §2.2 fill+text pairings (never a raw brief color). **Layer 1 only** — these are internal operational states, never shown to the End Traveler.

**Navigation/sidebar** — Super Admin Console left nav (21.6: Consultants, Suppliers, Ad Accounts, AI Governance Logs, Global Reporting) and Consultant Dashboard tabs (21.5: Top Packages, Pending Quotations, Active Campaigns) — `secondary-900` (navy) background, white/neutral-300 text, `primary-600` active-item indicator. **Layer 1 only, always.**

---

## 8. Iconography and imagery direction

**Icons: Lucide** — stroke-based, 24px default, 1.5–2px stroke weight matching Inter's geometry, MIT-licensed and bundled (not a CDN, same self-hosting reasoning as §4's fonts), and it's the natural pairing for a shadcn-convention component set (§6) if/when the team migrates to the full CLI. Icons are never tenant-supplied or tenant-colored on Layer 2 — they render in fixed neutral/semantic colors regardless of theme.

**Supplier photos** (hotels/activities — PRD §10, wildly inconsistent quality/aspect ratio/color cast across Hotelbeds/STUBA/TBO/HBActivities): every photo renders inside a **fixed frame component**, never a raw `<img>` — `aspect-ratio: 4/3` (list views) or `16/9` (detail hero), `object-fit: cover`, `neutral-100` placeholder/skeleton shown until loaded, `1px neutral-200` border, `8px` radius, uniform across every card regardless of which Consultant's theme surrounds it. Any overlaid text (e.g. a price badge bottom-left on the image) reuses the exact §3.3 scrim mechanism rather than a separate ad hoc solution — same problem, same fix, applied at a component level instead of a whole-page level.

---

## 9. Motion/interaction guidelines

Fixed timing tokens, identical on both layers (motion is not a tenant-customizable dimension — not granted by PRD §13.2, and inconsistent per-tenant animation timing would read as unpolished regardless of color theme):

| Token | Value | Use |
|---|---|---|
| `--motion-micro` | 120ms ease-out | Hover/active state changes |
| `--motion-standard` | 200ms ease-out | Panel open/close, dropdown |
| `--motion-page` | 250ms ease-in-out | Route-level transitions |

**Loading states:** skeleton placeholders (matching final content's approximate shape/size, not a generic spinner) for the Search Dashboard results panel (21.1's explicit loading-state requirement) and any data-grid; a spinner only for button-level in-progress actions. `role="status"`/`aria-live="polite"` on async result containers (the existing `SearchDashboard.tsx` already does this correctly for its loading/error states — extend the pattern, don't replace it).

---

## 10. Per-screen visual notes (PRD Part 21)

| # | Screen | Layer | Notes |
|---|---|---|---|
| 21.1 | Search Dashboard | **1** | Consultant/User-only tool. Living reference implementation (§ Implementation). |
| 21.2 | Itinerary Builder | **1** | Alternate-selection side panel = Radix Dialog/Sheet, Layer 1 styled. |
| 21.3 | Package Builder | **1** | ATOL disclosure step (UK) uses fixed `warning`-mapped styling, not tenant color — it's a compliance gate, must never be softened by theme. |
| 21.4 | Booking & Payment Flow | **Mixed** | The data-entry flow itself (traveler form, price breakdown, payment method) is operated by the Consultant's User and is Layer 1. Its *output artifacts* — voucher, and for UK dynamic packages the ATOL certificate — are viewed by the End Traveler on the Consultant's domain and carry the Layer 2 themed header/logo around a Layer-1-styled (fixed price/line-item) document body. |
| 21.5 | Consultant Dashboard | **1** | Onboarding checklist replaces empty charts for new Consultants (21.5's explicit empty state) — styled as an info-mapped card, not tenant color. |
| 21.6 | Super Admin Console | **1** | This is where Layer 2 tokens are *configured* (branding picker, §3.6) but the console chrome itself is pure Adren. |
| 21.7 | Wallet & Billing | **1** | Credit-limit breach warning (21.7) uses fixed `warning`/`error` mapping — never themeable, same reasoning as the ATOL gate. |
| 21.8 | Campaign Builder | **1** | Status stepper uses fixed status-color mapping (§7). |
| 21.9 | PNR/Booking Search | **1** | |
| 21.10 | Notification Preferences | **1** | |

**Gap found while writing this document, flagged for PM/PRD follow-up (see §12):** Part 21 has no dedicated screen spec for the actual **Consultant storefront / quotation / voucher page** — the Layer 2 surface the End Traveler (persona 3.4) receives "via the Consultant's domain." §13.2's branding fields and persona 3.4 both clearly require this surface to exist, but it isn't one of the 10 numbered screens. The implementation below includes a placeholder `ConsultantStorefront` screen built from this document's §3 architecture, but its actual content/layout beyond the themed header+hero should be treated as provisional until Part 21 gets an addition for it.

---

## 11. Accessibility baseline

- **Focus states:** 2px solid ring, `info-600`, 2px offset, on every interactive element on both layers — **fixed color, never tenant-themed** (§7's Button spec states this explicitly; it generalizes to every focusable component). This is the concrete restatement of the point made throughout §3: the contrast-safety algorithm is what keeps Layer 2's arbitrary tenant color from becoming an accessibility regression *for text*, and a fixed (never-themed) focus ring is what keeps it from becoming one *for keyboard navigation* — the two mechanisms cover the two ways tenant input could otherwise degrade accessibility.
- **Keyboard nav:** Radix primitives (Dialog, Select, Dropdown) handle focus trapping/return and arrow-key navigation out of the box; custom components (Badge, Card, Button) follow standard tab-order/Enter-Space activation.
- **ARIA patterns:** async result containers use `role="status"`/`role="alert"` + `aria-live` (already correctly started in `SearchDashboard.tsx`); the Campaign status stepper (21.8) uses `aria-current="step"`; form errors are linked to their input via `aria-describedby`, not color alone.
- **Tap targets:** minimum 44×44px on any control that must work at the `md`/`sm` breakpoints (§5), even though the primary design target is desktop.
- **Color is never the only signal:** status badges pair color with a text label (never a bare color dot); error states pair `error-600` styling with an icon + text message, not border color alone.

---

## 12. Open decisions requiring sign-off

These are called out explicitly rather than decided silently:

1. **Dark mode is out of scope for MVP** (§2.4). Low risk, but it's a scope call — confirm before anyone assumes it's coming later "for free."
2. **Tenant theming is read strictly as PRD §13.2's literal four fields** (logo, background, two text colors, domain) — Consultants do **not** get a brand/accent color for buttons/CTAs on their storefront; those stay Adren purple (§3.1, §7). This is the interpretation this whole architecture is built on. If the actual product intent is broader ("Consultants should feel like it's *their* site," not just their logo+photo+two text colors on an Adren-purple site), that's a materially bigger theming surface and changes §3's token set — needs explicit confirmation before more Layer 2 UI is built on the narrow reading.
3. **Contrast-safety cannot be mathematically guaranteed for 100% of arbitrary uploads** — specifically, high-internal-variance images within the sampled text zone (§3.3's flagged limitation). Mitigation (scrim + fallback-chain + visual live-preview backstop) is practical, not absolute. Worth deciding whether that residual risk is acceptable for MVP or whether a stronger (and more expensive — real segmentation/OCR-region analysis) approach is required before GA.
4. **Part 21 has no dedicated Consultant storefront/quotation/voucher screen spec** (§10) — the implementation below includes a provisional placeholder; the real screen spec needs a PRD addition and design pass beyond what this document infers.
5. **Hindi/regional-language UI scope is ambiguous in PRD §13.3** ("English + Hindi/regional consideration"). This document treats it as font-fallback-ready (Noto Sans Devanagari included, §4) but does **not** implement actual string translation/i18n infrastructure in MVP — confirm whether that's correct or whether i18n needs to be pulled into this MVP window.
6. **No RTL requirement currently modeled.** None of the six listed markets (India, Australia, UK, USA, Dubai/UAE, Denmark — PRD header) require an RTL UI language in-scope (Dubai's listed currency is AED but the language isn't specified as Arabic-UI in §13.3, which says expansion markets are "English-primary"). If Arabic UI is ever required for the Dubai market, RTL layout mirroring is a separate, larger effort not covered by this document — flagging now so it isn't assumed to be a small follow-on.

---

## 13. Implementation reference

- `frontend/src/shared/design-system/tokens.css` — Layer 1 CSS custom properties (§2), static, defined once.
- `frontend/tailwind.config.ts` — Tailwind config mapping utility classes to the Layer 1 tokens above.
- `frontend/src/shared/theming/contrastSafety.ts` — the §3.3 pure-function algorithm, unit tested.
- `frontend/src/shared/theming/resolveTenantTheme.ts` — wraps the algorithm for the two tenant text-color zones (header/hero).
- `frontend/src/shared/theming/sampleImageRegion.ts` — canvas-based image-region sampling (§3.3 step 1).
- `frontend/src/shared/theming/tenantThemeStore.ts` — which Consultant's resolved theme is active, as a Zustand store per `doc/architecture/RULES.md` §7.1 (not a React Context — see that file's doc comment for why this and FES-02's provider slot are deliberately different mechanisms).
- `frontend/src/shared/theming/TenantThemedSurface.tsx` — runtime Layer 2 CSS-custom-property injection onto a scoped wrapper element.
- `frontend/src/shared/providers/AppProviders.tsx` — FES-02's provider-stack slot (currently empty; theming doesn't occupy it, see above).
- `frontend/src/features/search-dashboard/` — restyled with Layer 1 tokens as the living reference.
- `frontend/src/features/consultant-storefront/` — placeholder Layer 2 screen demonstrating the contrast-safety algorithm against a sample uploaded background/color (see §12 item 4 for its provisional status).
- `.claude/skills/frontend-design-system/SKILL.md` — actionable Layer 1/Layer 2 rules distilled from this document.
