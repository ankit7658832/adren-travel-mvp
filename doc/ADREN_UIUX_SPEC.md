# ADREN TRAVEL — End-to-End UI/UX Specification
## (Figma/AI-Prototyping-Ready Edition)

**Version:** 1.1 — reconciled against `doc/DESIGN.md` (color/token authority resolved, SCR-25 added; see that document's §12 for open items this reconciliation surfaced)
**Prepared as:** Full product UX architecture — every screen, every state, every component style
**Audience:** AI design/prototyping tools (Figma AI generators, "Antigravity"-class tools), frontend developers, business stakeholders
**Source of truth:** `doc/PRD_v2_detailed.md` (all section references below point here) for functional/content scope — this document extends PRD Part 21 into full visual/interaction detail and fills screens the PRD's product list implies but never specified. **For every color, type, spacing, and component token, the source of truth is `doc/DESIGN.md`** — this document references those tokens by name and never restates a hex value or a token definition of its own (see Part 2).

---

# 0. How to Use This Document

This document is written so three different readers get what they need from the same source, without needing three separate docs:

- **An AI design tool** (Figma generator, prototyping AI) should treat **Part 2 (Design Foundations)** as its token/style source, **Part 4 (Screen Inventory)** as its frame list, and each screen's **Section spec** (Parts 5–13) as a literal build brief — layout regions, components used, exact colors/states, and content fields are all specified so a frame can be generated directly from each screen's spec without additional invention.
- **A developer** should treat the component state tables (buttons, inputs, badges, modals) as the literal CSS/token values to implement, and cross-reference each screen's **PRD/Story reference** line to find the corresponding backend contract.
- **A business reader** should read each screen's **Purpose** line and skip the detailed state tables — every screen leads with a plain-language sentence before the technical spec begins.

---

# 1. Gap Analysis — What This Document Adds Beyond PRD Part 21

PRD Part 21 specified 10 screens at a structural level. Building this out to full UX revealed **17 additional screens/flows** implied by the product's own scope (5 product types × search+detail, a booking pipeline with distinct steps, and auth/error handling that has to exist for the product to function at all) but never explicitly written down. These gaps are called out here so nothing is silently invented without you seeing it:

| Missing from PRD Part 21 | Why it's required | Added as |
|---|---|---|
| Login screen | No screen in the PRD lets anyone authenticate — Section 6's role matrix and Section 13's Consultant accounts are meaningless without one | SCR-00 |
| Forgot/Reset Password | Standard companion to any login flow | SCR-00b |
| Per-product **List** pages (Hotel, Flight, Transfer, Cruise, Activity) | PRD Section 21.1/21.2 describe search results and itinerary cards generically ("per-location cards"); a Consultant comparing 15 hotel options needs a dedicated list/filter view, not a card stack | SCR-04 through SCR-08 |
| Per-product **Detail** pages (Hotel+Amenities, Flight, Transfer, Cruise, Activity) | Same gap — PRD never specifies what happens when a Consultant clicks into one specific option | SCR-09 through SCR-13 |
| Pax/Traveler Details page | PRD Section 20.10 defines the Traveler Profile data model but no screen collects it | SCR-14 |
| Rate/Price Re-validation ("Rate Check") page | PRD Sections 9.4, 10.2.1, 22.4 all describe stale-rate/re-validation logic; no screen was ever specified for it | SCR-15 |
| Global Error Modal | PRD Section 21.1 mentions error states inline per screen; a B2B tool handling 9 external supplier APIs needs one consistent, reusable error-modal pattern, not 10 different ad hoc error UIs | SCR-16 (component, not a routed screen) |
| Booking Confirmation page (distinct from the Booking & Payment *flow*) | PRD Section 21.4 bundles the whole flow together; confirmation is its own state with its own content (voucher, ATOL cert, next actions) and deserves its own frame | SCR-17 |
| Consultant Storefront / Quotation & Voucher page | PRD §13.2's branding fields and persona 3.4 ("receives itineraries/vouchers via Consultant's domain") both require a Layer 2 surface for this to happen on, but no PRD Part 21 screen and no earlier version of this document ever specified it — `doc/DESIGN.md` flagged the same gap independently from its own Layer 2 architecture work. Added here once, for real, in §12.3 | SCR-25 |

Total screen count in this document: **10 PRD-specified + 18 gap-filled = 28 screens/flows.**

---

# 2. Design Foundations

**This section no longer defines any color, type, spacing, or component value.** An earlier version of this document maintained its own copy of the palette, type scale, and component-state tables — that copy drifted from `doc/DESIGN.md` (most visibly: this document had blue as the primary action color where DESIGN.md's contrast-verified, category-convention-justified assignment is purple; several of this document's "AA-safe" colors, e.g. `#B8860B` for warning text, actually fail DESIGN.md's own AA audit). That drift is the reason this reconciliation pass exists. Going forward, every token below is a pointer, not a definition, so the two documents cannot silently diverge a second time.

## 2.1 Colors

**Defined in `doc/DESIGN.md` §1–§2.** Use DESIGN.md's token names directly (`--color-primary-600`, `--color-success-700`, etc.) — never a hex value — in any screen spec, including new ones added to this document later. Two points worth restating here because they change what earlier drafts of this document assumed:

- **Purple (`--color-primary-600`, `#5B2A9E`) is primary/CTA; blue (`--color-info-600`, `#2E6FE0`) is info**, not the reverse. DESIGN.md §1 has the full rationale (purple has no competing semantic convention in the brief's palette; blue is oversaturated in travel-tech/B2B SaaS).
- **Focus rings are always `--color-info-600`, fixed on both layers, never the primary/tenant color** (DESIGN.md §7, §11). This document's earlier component tables incorrectly showed focus rings using the primary token — a real inconsistency this reconciliation fixes, not just a renaming.

## 2.2 White-Label Theming (Layer 2 — tenant-controlled surfaces)

**Architecture defined in `doc/DESIGN.md` §3** — read that section in full before touching any Layer 2 screen; it's the core deliverable of the design system, not a summary-able aside. Per PRD §13.2, the tenant-overridable set is exactly four fields (DESIGN.md §3.1): `tenant.logoUrl`, `tenant.backgroundImageUrl`/`tenant.backgroundColor`, `tenant.textColorPrimary`, `tenant.textColorSecondary` (this document's earlier draft omitted the secondary text color field entirely — another drift instance). Everything else, including all semantic colors, all price displays, and CTA/button fill color, stays fixed regardless of tenant input (DESIGN.md §3.1's explicit list). The contrast-safety scrim/fallback algorithm (DESIGN.md §3.3) is what guarantees this; per-screen notes below only say *which* zones on a given screen read tenant color, never redefine the algorithm.

## 2.3 Typography

**Defined in `doc/DESIGN.md` §4.** Font is Inter (self-hosted) everywhere, on both layers, never tenant-overridable. Type scale tokens (`--text-xs` through `--text-4xl`) and their px/line-height values live there — this document previously kept a separate, slightly different scale (e.g. a 32px "Display" style DESIGN.md doesn't define); use DESIGN.md's scale, mapping this document's earlier semantic names as: page-level headers → `--text-3xl`/`--text-4xl`, screen titles → `--text-2xl`, section headers → `--text-xl`, card titles → `--text-lg`, default body → `--text-base`/`--text-sm`, captions/meta → `--text-xs`.

## 2.4 Spacing & Grid

**Defined in `doc/DESIGN.md` §5**, including the 12-column/1280px-max-width/24px-gutter content grid (added to DESIGN.md §5 by this reconciliation, since it previously lived only in this document). Breakpoint tokens and behavior per breakpoint are DESIGN.md §5's table — use those instead of this document's earlier, slightly different breakpoint list. The desktop-first-except-traveler-facing-screens rule DESIGN.md §5 now states explicitly is unchanged from what this document said: Consultant/Super Admin screens (SCR-01–SCR-16, SCR-19–SCR-24) target desktop/laptop first; traveler-facing Layer 2 surfaces (SCR-17, SCR-18, SCR-25) must be fully usable at `sm`.

## 2.5 Core Components

**Component definitions — variants, sizes, states, Radix primitive choices, exact Layer 1/2 applicability — are defined in `doc/DESIGN.md` §6–§7 (Button, Form inputs, Tables, Cards, Modals, Status/badge indicators, Navigation) and §8 (icons).** This document does not redefine what any component looks like. What follows is the usage information DESIGN.md's component specs don't carry — which entity statuses map to which badge, and this document's Global Error Modal usage rules — kept here because it's screen/content-specific, not a component style.

### Entity status → badge mapping

Every row below uses one of DESIGN.md §2.2's two AA-safe recipes — **tint background + darkened (`-700`) text**, per the Badge component's `status` enum prop (DESIGN.md §7) — never a raw brief color as text-on-white or text-on-tint. (Two corrections from this document's earlier draft: "Rejected" previously implied `error-600` text on an `error-50` tint, which DESIGN.md §2.2 documents as failing AA at 4.04:1 — it must be `error-700`, 6.05:1. "Quotation" previously used the pre-reconciliation purple/blue-secondary token, now resolved to `--color-primary-600`'s family.)

| Entity status | Badge recipe | Text |
|---|---|---|
| Draft (Itinerary) | `--color-neutral-200` fill, `--color-neutral-700` text | "Draft" |
| Quotation | `--color-primary-50` tint, `--color-primary-700` text | "Quotation" |
| Booked/Confirmed | `--color-success-50` tint, `--color-success-700` text | "Confirmed" |
| Cancelled | `--color-neutral-200` fill, `--color-neutral-700` text, strikethrough label | "Cancelled" |
| Pending Approval (Campaign) | `--color-warning-50` tint, `--color-warning-700` text | "Pending Approval" |
| Live (Campaign) | `--color-success-50` tint, `--color-success-700` text | "Live" |
| Rejected/Suspended | `--color-error-50` tint, `--color-error-700` text | "Rejected" |

Local DMC vetting statuses (unvetted/pending/approved/rejected, DESIGN.md §7) follow the same recipe pattern: unvetted/pending → `neutral`/`warning`, approved → `success`, rejected → `error`.

### Global Error Modal (SCR-16 — used across every screen)

- Trigger: any unhandled API error, supplier timeout (PRD §10.2 error tables), or a blocking validation failure.
- Layout: DESIGN.md §7's Modal spec (Radix Dialog, `surface` background, focus-trapped) applies. Centered, max-width 480px, `--color-error-600` 4px top border, alert-circle icon in `--color-error-600`, H3 title, body text explaining the failure in plain language (never a raw stack trace or supplier error code), one primary action ("Retry") and one ghost action ("Dismiss" or "Go back").
- **Always Layer 1 styled, regardless of which screen or layer triggered it** — this is DESIGN.md §7's Modal rule applied to this specific modal (see DESIGN.md §10.1, SCR-16), not a separate decision made here.
- Non-blocking variant (toast, per story FES-10): bottom-right, 4-second auto-dismiss for non-critical async failures (e.g., a background notification-send failure); the blocking modal variant is reserved for anything that stops the user's current task (payment failure, rate expired, no supplier availability at all).

---

# 3. Global Navigation Shell

Two distinct shells, matching PRD Section 6's role matrix:

**Consultant/User Shell:** left sidebar (Search, Itineraries, Packages, Bookings/PNR Search, Wallet, Campaigns, Settings), top bar (Consultant's white-label logo — Layer 2 — plus Adren "Powered by" wordmark small/fixed, user menu, notification bell).

**Super Admin Shell:** left sidebar (Consultants, Suppliers, Ad Accounts, AI Governance Logs, Global Reporting), top bar fixed Adren branding only (no tenant theming — Layer 1, always).

Both: collapsible sidebar at `md` breakpoint and below, becoming a bottom tab bar on `sm`.

---

# 4. Complete Screen Inventory

| ID | Screen | PRD/Story Ref | Layer | Section |
|---|---|---|---|---|
| SCR-00 | Login | *(gap — see Part 1)*, feeds FND-01/FES-07 | Layer 1 | 5.1 |
| SCR-00b | Forgot/Reset Password | *(gap)* | Layer 1 | 5.2 |
| SCR-01 | Search Dashboard | PRD §21.1, FND-13 | Layer 1 | 6.1 |
| SCR-02 | Itinerary Builder | PRD §21.2, FND-16 | Layer 1 | 6.2 |
| SCR-03 | Alternate-Selection Panel | PRD §21.2 (sub-component) | Layer 1 | 6.2 |
| SCR-04 | Hotel List | *(gap)* | Layer 1 | 7.1 |
| SCR-05 | Flight/Transport List | *(gap)* | Layer 1 | 7.2 |
| SCR-06 | Transfer List | *(gap)* | Layer 1 | 7.3 |
| SCR-07 | Cruise List | *(gap)* | Layer 1 | 7.4 |
| SCR-08 | Activity List | *(gap)* | Layer 1 | 7.5 |
| SCR-09 | Hotel Details + Amenities | *(gap)* | Layer 1 | 8.1 |
| SCR-10 | Flight Details | *(gap)* | Layer 1 | 8.2 |
| SCR-11 | Transfer Details | *(gap)* | Layer 1 | 8.3 |
| SCR-12 | Cruise Details | *(gap)* | Layer 1 | 8.4 |
| SCR-13 | Activity Details | *(gap)* | Layer 1 | 8.5 |
| SCR-14 | Pax/Traveler Details | *(gap)*, feeds PRD §20.10 | Layer 1 | 9 |
| SCR-15 | Rate Check / Re-validation | *(gap)*, PRD §9.4/§22.4 | Layer 1 | 10 |
| SCR-16 | Global Error Modal (component) | *(gap)* | Layer 1 | 2.5 |
| SCR-17b | Package Builder | PRD §21.3 | Layer 1 | 11 |
| SCR-18 | Booking & Payment Flow | PRD §21.4, BOK-13 | Layer 1/2 mixed | 12.1 |
| SCR-17 | Booking Confirmation | *(gap, split from §21.4)* | Layer 1/2 mixed | 12.2 |
| SCR-25 | Consultant Storefront / Quotation & Voucher | *(gap)*, PRD §13.2/persona 3.4 | Layer 1/2 mixed | 12.3 |
| SCR-19 | Consultant Dashboard | PRD §21.5 | Layer 1 | 13.1 |
| SCR-20 | Super Admin Console | PRD §21.6 | Layer 1 | 13.2 |
| SCR-21 | Wallet & Billing | PRD §21.7 | Layer 1 | 13.3 |
| SCR-22 | Campaign Builder | PRD §21.8 | Layer 1 | 13.4 |
| SCR-23 | PNR/Booking Search | PRD §21.9 | Layer 1 | 13.5 |
| SCR-24 | Notification Preferences | PRD §21.10 | Layer 1 | 13.6 |

---

# 5. Auth Flow

## 5.1 SCR-00 — Login

**Purpose (business):** The front door — every Consultant, User, and Super Admin starts here before they can do anything else in the product.

**Layout:** Centered single-column card (max-width 400px) on a full-bleed background using **Adren's own default background** (Layer 1 — this screen is never tenant-themed, since a Consultant hasn't been identified yet at login time). Adren logo centered above the card.

**Fields:** Email (text input), Password (password input, show/hide toggle icon), "Remember me" checkbox, primary button "Log In" (full width), text link "Forgot password?" (right-aligned under password field), text link "Contact your administrator" (small, centered below the card, for Users who don't self-register).

**States:**
- Default: empty fields, button enabled (validation on submit, not on every keystroke)
- Loading: button shows inline spinner + "Logging in…", fields disabled
- Error: SCR-16 inline variant — red text directly under the password field ("Incorrect email or password"), never specifying which field was wrong (standard security practice)
- Success: redirect to role-appropriate landing screen (Consultant/User → SCR-01 Search Dashboard; Super Admin → SCR-20 Super Admin Console)

## 5.2 SCR-00b — Forgot/Reset Password

**Purpose (business):** Lets someone regain access without contacting support for every password reset.

**Layout:** Same centered card pattern as SCR-00. Step 1: email input + "Send reset link" button. Step 2 (after email link clicked, separate route): new password + confirm password fields + "Reset Password" button.

**States:** Default, loading, success ("Check your email" confirmation message — same screen, replaces the form, doesn't reveal whether the email exists in the system), error (rate-limited/expired-link messaging via SCR-16 inline variant).

---

# 6. Search & Itinerary Building

## 6.1 SCR-01 — Search Dashboard

**Purpose (business):** Where a Consultant starts building a trip — enter destinations and dates, see a map, get instant per-location default suggestions.

**Layout:** Two-column at `lg`+ — left 40% map panel (pins per searched location), right 60% results panel (per-location cards). Collapses to stacked single-column (map above, results below) at `md` and below. Top of results panel: the multi-select location search box (chips-style input — each location becomes a removable chip as it's typed/selected from autocomplete), date range picker, pax selector, "Search" primary button.

**States:**
- Default: empty search box, map shows a default world/region view, no results panel
- Loading: skeleton cards in results panel (3 gray pulsing rectangles), map pins fade in as each location resolves
- Success: one card per location, each showing its auto-selected default product (small thumbnail, name, price, "Auto-selected: Best available match" caption in `--color-neutral-500`, 12px) and a "Change" ghost button opening SCR-03
- Empty (partial): a location with zero inventory shows a distinct card state — neutral gray background, alert-triangle icon, "No inventory available for this location" text, no thumbnail/price — never simply omitted from the map or list
- Error: SCR-16 modal if the entire search fails; per-location empty state (above) if only some locations fail

## 6.2 SCR-02 — Itinerary Builder

**Purpose (business):** The main workspace for assembling a full multi-product trip before saving it as a quotation.

**Layout:** Persistent left rail: ordered list of locations (drag-to-reorder). Main area: one expandable card per location, each containing up to 5 product-category rows (Hotel/Flight/Transfer/Cruise/Activity — only categories relevant to that location show), each row showing the currently-selected item + price + a "Change" button opening SCR-03. Persistent bottom bar: running total price (large, `--color-secondary-900`, updates live), currency selector, "Complete with AI" button (DESIGN.md §7 `secondary` variant — white fill, `--color-neutral-300` border, `--color-neutral-900` text), "Save as Quotation" primary button.

Note on the "Complete with AI" button: an earlier draft of this document gave it a solid purple fill to tie it to the AI-assist brand identity, distinct from the primary CTA. Now that purple is resolved to `--color-primary-600` (the primary/CTA color, DESIGN.md §1), a solid purple fill would make two adjacent buttons both read as "the" primary action. Resolved here by using DESIGN.md's existing `secondary` (neutral outline) variant instead, and carrying the AI-assist identity through the icon and the AI-in-progress row highlight below rather than through button fill color. DESIGN.md §7 doesn't currently define a colored/accent button variant — if a distinct "AI-accent" button treatment is wanted beyond this, that's a new addition to DESIGN.md's component set, not something to decide here.

**States:** Default (all rows showing auto-selections), loading (a specific row shows a small inline spinner when AI is actively completing just that segment — not a full-screen block), error (a row shows an inline error chip "Could not find a match" rather than blocking the whole builder), AI-in-progress (rows being actively filled by AI show a subtle `--color-primary-600` left-border highlight until accepted).

## 6.3 SCR-03 — Alternate-Selection Panel

**Purpose (business):** Lets a Consultant swap the system's default pick for something else, seeing all real options side by side.

**Layout:** Right-side slide-in drawer (480px wide), filter/sort bar at top (price, rating, supplier), scrollable list of option cards below (thumbnail, name, key spec — room type/cabin class/vehicle type per product — price, supplier badge), each with a "Select" primary button.

**States:** Loading (skeleton list), populated, empty ("No other options available for this category at this location" — plain-language, centered, no icon needed since it's a drawer not a full page).

---

# 7. Per-Product List Pages

Shared layout pattern across all 5 — described once, with per-product field differences noted.

**Shared layout:** Top filter bar (price range slider, star/category filter, supplier filter, sort dropdown). Left: filter sidebar (collapsible on `md`-). Main: result list — table view on desktop (`lg`+), card view on tablet/mobile. Each row/card: thumbnail, name, key spec, supplier badge, price, "View Details" button (→ corresponding Detail page) and a lightweight "Add to Itinerary" button (skips Details for a fast-path add).

## 7.1 SCR-04 — Hotel List
Columns/fields: Property name, star rating (icon row), location/area, room type preview, meal plan badge (RO/BB/HB/FB/AI), cancellation deadline (relative, e.g. "Free cancellation until Jul 20"), net price is never shown here (Consultant-facing sell price only, per PRD §12.1 markup visibility rules), supplier badge (Hotelbeds/STUBA/TBO/Local DMC/BYOS — small colored dot + label, not a logo, to avoid implying co-branding with suppliers).

## 7.2 SCR-05 — Flight/Transport List
Fields: Airline + flight number, departure/arrival time (large, primary info), duration, cabin class badge, baggage allowance icon+text, fare-expiry countdown badge (`--color-warning-500` fill + `--color-secondary-900` text — DESIGN.md §2.2's fill+dark-text recipe — once under 15 minutes remaining; flights expire faster than other products per PRD §10.2.4).

## 7.3 SCR-06 — Transfer List
Fields: Vehicle type icon, pickup/dropoff labels (with a small inline map-pin icon linking each to the itinerary's location), estimated duration, capacity (passenger count icon).

## 7.4 SCR-07 — Cruise List
Fields: Cruise line + ship name, sailing dates, cabin category, port count badge ("5 ports"), passenger-document-required icon (small passport icon) when applicable per PRD §20.5.

## 7.5 SCR-08 — Activity List
Fields: Activity name, duration, available time slots shown as small pill buttons directly in the row (selecting one is required before "Add to Itinerary" activates — since headcount/slot must be fixed at booking per PRD §10.2.7), headcount stepper.

---

# 8. Per-Product Detail Pages

Shared layout pattern: large image gallery/hero at top (carousel, 60% viewport height max), sticky right-side summary card (price, key facts, "Add to Itinerary" primary button) that stays visible while scrolling the left content column.

## 8.1 SCR-09 — Hotel Details + Amenities
Left column sections: Overview (description), **Amenities** (icon grid — WiFi, pool, gym, parking, etc., grouped by category), Room Types (expandable list, each with its own price/select), Cancellation Policy (plain-language rendering of the deadline, not raw supplier policy text), Location (embedded static map), Reviews (if supplier provides — else section omitted, not shown empty).

## 8.2 SCR-10 — Flight Details
Sections: Full itinerary timeline (departure → arrival, layovers if any, shown as a vertical timeline component), Fare Rules (refundable/non-refundable badge, change-fee summary in plain language), Baggage (icon breakdown per class), fare-expiry countdown repeated prominently near the summary card.

## 8.3 SCR-11 — Transfer Details
Sections: Route map (pickup→dropoff line on embedded map), Vehicle details (capacity, luggage allowance, meet-and-greet indicator if applicable), Cancellation deadline (typically shorter window, flagged distinctly per PRD §10.2.5 — shown in `--color-warning-700` text, the AA-safe body-text variant per DESIGN.md §2.2, if under 24 hours).

## 8.4 SCR-12 — Cruise Details
Sections: Full sailing itinerary (port-by-port list, per PRD §20.5's flattened port metadata, shown as a horizontal timeline/map), Cabin Category comparison table, **Passenger Document Requirement notice** (prominent callout box: `--color-warning-50` background, `--color-warning-700` text, `--color-warning-500` left border — DESIGN.md §2.2's tint+darkened-text recipe — since this blocks booking completion until satisfied at SCR-14).

## 8.5 SCR-13 — Activity Details
Sections: Description, Time slot selector (same pill pattern as SCR-08, larger here), Headcount stepper, Meeting point (map + text), Inclusions/Exclusions (two-column icon list, checkmark vs. x-mark).

---

# 9. SCR-14 — Pax/Traveler Details

**Purpose (business):** Collects who's actually traveling — names, documents — before a booking can be confirmed.

**Layout:** Stepper/accordion, one section per traveler (added via "+ Add Traveler" ghost button), each section: Name, Date of Birth, Nationality (dropdown), and — **conditionally shown** only when the itinerary contains a cruise or international product per PRD §20.5/§20.10 — Passport Number, Passport Expiry, plus a document-upload dropzone (drag-and-drop or click, accepts PDF/JPG/PNG, shows thumbnail once uploaded).

**States:** Default (one empty traveler section), validation error (inline per field, red border + caption text, per the Input error state in 2.5), a locked/disabled state for fields already filled from a previously-saved Traveler Profile (with an "Edit" ghost button to unlock), completion checkmark badge on each traveler section once all required fields for that itinerary's products are satisfied.

---

# 10. SCR-15 — Rate Check / Re-validation

**Purpose (business):** The safety check right before payment — confirms every price is still accurate, since supplier rates can expire between search and checkout (per PRD §9.4, §22.4).

**Layout:** Simple vertical list, one row per line item, each showing: original price (struck through only if changed), current re-validated price, a status icon (`--color-success-500` check "Price confirmed" / `--color-warning-700` triangle "Price changed" / `--color-error-600` x "No longer available"). The warning icon uses `-700`, not `-500`, because `--color-warning-500` (raw gold) is 1.82:1 on white and fails even the 3:1 non-text/icon threshold per DESIGN.md §2.2 — `-700` (5.54:1) is the floor that's actually legible directly on a white row. Summary bar at bottom: total (updated), a required checkbox "I confirm the updated total" if any price changed, primary button "Continue to Payment" (disabled until any changed-price items are acknowledged, and replaced with "Remove & Continue" if any item is fully unavailable).

**States:** Loading (all rows show a spinner while re-validating, typically under 2 seconds), all-confirmed (green checks throughout, button enabled immediately), some-changed (warning rows highlighted, checkbox required), unavailable (that row shows a "Remove" button instead of a price, blocking checkout until resolved — mirrors PRD §22.4's stale-rate acceptance criterion directly).

---

# 11. SCR-17b — Package Builder

**Purpose (business):** Turns a saved trip into a reusable, sellable package with its own pricing and validity window.

**Layout:** Form over a read-only summary of the source Quotation (collapsed itinerary card). Fields: Package Name, Description (rich text, basic formatting only), Validity Start/End (date range), Max Pax (stepper), Base Price (auto-filled, read-only), Markup (editable percentage or flat-fee toggle), computed Sell Price (large, live-updating). **Conditional block:** if the package combines a flight + hotel and the Consultant's market is UK (PRD §17.2), an additional required section appears — "ATOL Disclosure" — with the disclosure text and a mandatory checkbox; the "Publish" button stays disabled until this is checked, matching PRD §22.3's blocking requirement exactly.

**States:** Draft (editable), validation-blocked (Publish button disabled + inline reasons listed above it, e.g. "ATOL disclosure required"), published (success toast, package now shows a "Promote this Package" secondary button leading to SCR-22).

---

# 12. Booking Pipeline

## 12.1 SCR-18 — Booking & Payment Flow

**Purpose (business):** Where the traveler-facing (or Consultant-on-behalf-of-traveler) checkout actually happens.

**Layout:** Multi-step within one screen (stepper header: Traveler Details → Rate Check → Payment): Traveler Details reuses SCR-14, Rate Check reuses SCR-15, final step is Payment — method selector (Stripe card element / Wallet / On-Account radio group), price breakdown (collapsible net/markup detail per Consultant's visibility setting, per PRD §21.4), "Confirm & Pay" primary button.

**Layer note:** the price breakdown and payment form stay Layer 1 (fixed, trustworthy — tenant theming must never touch a price display, per the DESIGN.md rule); only the surrounding header/logo chrome is Layer 2 if this flow is reached via a Consultant's white-label domain.

**States:** Each step has its own default/loading/error per its component above; final Payment step adds a processing state (button shows spinner + "Processing payment…", all fields locked) and routes to SCR-17 on success.

## 12.2 SCR-17 — Booking Confirmation

**Purpose (business):** The payoff screen — confirms the booking succeeded and gives the traveler/Consultant what they need next.

**Layout:** Centered success state — large `--color-success-500` checkmark icon (large-icon use is exactly what `success-500` is designated for per DESIGN.md §2.2), "Booking Confirmed" H1, booking reference (large, monospace font for scanability), summary card (dates, travelers, total paid), action buttons: "Download Voucher" (primary), "Download ATOL Certificate" (secondary, **only rendered when applicable** per PRD §20.11/§17.2 — never shown as a disabled/grayed button when not applicable, simply absent), "View Booking" (ghost, → SCR-23 detail view).

**States:** This screen only has a success state by definition — payment failures are handled back in SCR-18, not here. A slow-voucher-generation edge case shows the summary immediately with the "Download Voucher" button in a brief loading state (spinner replacing the download icon) rather than blocking the whole confirmation screen.

## 12.3 SCR-25 — Consultant Storefront / Quotation & Voucher

**Purpose (business):** What an End Traveler (persona 3.4) actually sees when they open the link a Consultant/User sends them — a quotation to review before booking, or a voucher after booking. Persona 3.4 says the End Traveler "does not log in during MVP; receives itineraries/vouchers via Consultant's domain" — this screen is that receiving surface. Neither PRD Part 21 nor an earlier version of this document specified it, even though §13.2's branding fields (logo, background, text colors, domain) and persona 3.4 both presuppose it exists; `doc/DESIGN.md` flagged the same gap independently while building its Layer 2 architecture. This is the real spec, built directly on that architecture (DESIGN.md §3), not the provisional placeholder DESIGN.md previously referenced.

**Layer:** Mixed — the same pattern DESIGN.md already applies to Booking & Payment (21.4) and this document applies to SCR-17: a Layer 2 themed header/hero wraps Layer 1, fixed-styled content. Concretely:

- **Layer 2 (tenant-themed) zone:** the nav/header band (Consultant's logo in the fixed `neutral-50` chip container, wordmark/tagline in the Consultant's resolved text color, small fixed "Powered by Adren" wordmark per Part 3's Global Navigation Shell pattern) and the hero band directly below it (background image/color + a short headline — e.g. the Consultant's tagline or "Your [Destination] Itinerary" — in the Consultant's resolved text color). Both zones run through DESIGN.md §3.3's contrast-safety pipeline exactly as specified there; nothing new is introduced here.
- **Layer 1 (fixed) zone — everything below the hero:** itinerary/price content, always on the fixed `surface` white Card component (DESIGN.md §7's Card spec: "cards stay this same fixed white/neutral styling regardless of tenant theme... only the page chrome around the cards is themed"). This is what keeps a price legible and trustworthy regardless of what text color or background image the Consultant picked.

**Layout (both states share this shell):**
1. Themed header + hero (Layer 2, as above).
2. Below the hero, on the fixed white surface: a status badge (Quotation: draft/sent/accepted/expired, or Booking: confirmed — using the §2.5 entity-status badge mapping), then a day-by-day / per-location itinerary summary reusing the same Card component the internal Itinerary Builder (SCR-02) uses for its line-item rows (thumbnail, name, key spec, price) — Consultant's sell price only, net rate never shown, per the same markup-visibility rule SCR-04 states for the internal product lists.
3. Price summary block: total (always `--color-neutral-900` on `surface`, DESIGN.md §3.1's explicit price-display rule — never the tenant text color), tax/fee breakdown if the Consultant's visibility settings show it (PRD §21.4).
4. Traveler/booking reference block (Voucher state only — see below).
5. Primary action area, content depending on state (below).
6. Footer: Consultant's contact details (phone/email, from their profile — not a tenant-styleable field, plain fixed text) and the same fixed "Powered by Adren" mark from the header, repeated per standard footer convention.

**States:**

- **Quotation state** (before booking/payment): status badge shows the Quotation sub-status (draft/sent/accepted/expired). Primary action area shows "Accept This Quotation" (primary button, fixed Adren purple per DESIGN.md §3.1 — CTA color is never tenant-colored even here) and a secondary "Request Changes" (DESIGN.md §7 `secondary` variant) that opens a simple message-to-Consultant form. **No self-checkout/payment happens on this screen** — per PRD persona 3.4, the End Traveler doesn't have an account and MVP scope has no self-service payment flow; "Accept" notifies the Consultant/User, who then runs the actual booking through SCR-18 on the traveler's behalf. If the quotation has expired (rate/validity lapsed), the primary action is replaced with a disabled state plus "This quotation has expired — contact [Consultant name] for an updated price," consistent with SCR-15's Rate Check treatment of expired pricing elsewhere in the product.
- **Voucher state** (after booking): status badge shows "Confirmed." Adds the booking reference (large, monospace, same treatment as SCR-17) and traveler names/dates block. Primary action area shows "Download Voucher" (primary) and, only when applicable per PRD §20.11/§17.2, "Download ATOL Certificate" (secondary, absent rather than disabled when not applicable — same rule as SCR-17).
- **Loading:** skeleton itinerary/price cards while the quotation/voucher record resolves; header/hero render immediately since tenant theme is resolved and cached independently (DESIGN.md §3.3 step 5 — resolved tokens are pre-computed, not fetched per-request).
- **Error (link invalid/expired/revoked):** SCR-16 Global Error Modal is not used here, since there's no "current task" to retry — instead a full-page fixed (Layer 1, per DESIGN.md's Modal-adjacent reasoning that error states are never tenant-themed) state: "This link is no longer valid — contact [Consultant name] for an updated link," with the Consultant's contact footer still shown (it's fixed content, not themed, so it stays legible even here).

**How this differs from SCR-17 (Booking Confirmation):** SCR-25's Voucher state and SCR-17 are content-equivalent — both show a confirmed booking's reference, summary, and download actions. The difference is delivery context, not content: SCR-17 is the *transient* screen reached immediately after "Confirm & Pay" succeeds inside the live app session (success-moment framing — large checkmark, "Booking Confirmed" H1); SCR-25 is a *durable*, independently-linkable document with no dependency on any prior app state, designed to be opened cold from an email/WhatsApp link on any device, any time after booking. SCR-25 also uniquely has the Quotation (pre-booking) state, which SCR-17 has no equivalent of at all. Whether these should actually be merged into one screen with two entry contexts is flagged as an open question in `doc/DESIGN.md` §12 item 8 — not decided here.

**PRD/Story reference:** PRD §13.2 (Branding Configuration), persona 3.4 (End Traveler), §21.4 (Booking & Payment Flow — this screen's Quotation state is the pre-payment counterpart), §20.11/§17.2 (ATOL certificate applicability), FND-06/FND-07 (branding save/propagation).

---

# 13. Remaining PRD-Specified Screens (Detailed)

## 13.1 SCR-19 — Consultant Dashboard
Card grid at top (Bookings This Month, GMV, Wallet Balance — each a stat card: large number, small trend indicator, `--color-neutral-500` label), tabbed section below (Top Packages / Pending Quotations / Active Campaigns — standard tab component, active tab underlined in `--color-primary-600`). New-Consultant empty state replaces the stat cards with an onboarding checklist card instead of zeroed charts, per PRD §21.5.

## 13.2 SCR-20 — Super Admin Console
Left nav (Consultants, Suppliers, Ad Accounts, AI Governance Logs, Global Reporting) + main content area per section. Consultant onboarding wizard (linked from here) is a multi-step form whose required fields change dynamically based on selected home market (India/Australia/UK/USA/Dubai/Denmark) per PRD §13.1/§17.1 — implement as a schema-driven field engine (per story FES-09), not hardcoded per-market branches in the UI layer. Supplier credential fields always render as masked (dot-obscured) with a small "Last changed by [name], [date]" caption.

## 13.3 SCR-21 — Wallet & Billing
Balance summary card (large balance, credit limit, available-to-spend computed value), filterable transaction ledger table below (Type/Amount/Date/Related Booking columns, filter dropdown by Type per PRD §20.12's enum), "Top Up" primary button. Credit-breach warning is an inline banner (`--color-warning-50` background, `--color-warning-700` text — DESIGN.md §2.2's tint+darkened-text recipe, full-width, dismissible) appearing above the ledger — not a modal — so it doesn't block viewing wallet history while resolving it.

## 13.4 SCR-22 — Campaign Builder
Package selector (searchable dropdown), campaign inputs form (audience description, budget slider + numeric input, duration date range), AI creative gallery (grid of generated image+copy variant cards, each with a checkbox to approve/select, `--color-primary-600` accent border on the currently-selected variant), status stepper component at top (Pending Approval → Pending Policy Review → Live/Rejected, using the status badge recipes from §2.5).

## 13.5 SCR-23 — PNR/Booking Search
Single prominent search field (centered, large, similar visual weight to SCR-00's login field — this is a "one job" screen), results list below once searched (booking reference, product-type icon row showing which categories are in that booking, status badge, "View" button), works identically regardless of underlying product type per PRD §22.8.

## 13.6 SCR-24 — Notification Preferences
Simple settings-pattern screen: toggle row per notification channel (Email — always on, disabled toggle since it's mandatory; Secondary Channel — dropdown WhatsApp/SMS, pre-selected per the Consultant's region default per PRD §15, overridable), save confirmation via toast (per FES-10), not a full-page reload.

---

# 14. Handoff Notes for AI Design/Prototyping Tools

- **Frame naming convention:** `[SCR-ID] Screen Name / State` — e.g. `SCR-01 Search Dashboard / Loading`, so every state in this document becomes its own frame, not just the default state.
- **Component library first:** build DESIGN.md §6–§7's component set (buttons, inputs, badges, error modal) as reusable Figma components/variants *before* assembling screens — every screen spec above references these by name rather than describing bespoke styling per screen.
- **Auto-layout:** all list/card layouts (search results, product lists, ledger tables) should use auto-layout with DESIGN.md §5's 4px-based spacing scale, not fixed pixel positioning, so density changes (e.g., more travelers, more line items) reflow correctly.
- **Tenant theming demo:** generate at least one Layer 2 example twice — once in default Adren theme, once with a sample tenant background/logo/text-color override — to prove the token substitution actually works visually, not just in spec. SCR-25 (Consultant Storefront/Quotation & Voucher) is the better candidate for this than SCR-17: it's the screen where tenant theming actually matters to the business (the thing a Consultant's client sees on the Consultant's own domain), whereas SCR-17 is reached inside the live app session regardless of theme.
- **Responsive frames:** generate each screen at `lg` (1280px) and `sm` (390px) at minimum; Consultant/Super Admin-only screens (SCR-19–24) can skip `sm` per the desktop-first note in DESIGN.md §5, but every traveler-facing screen (SCR-00, SCR-17, SCR-18, SCR-25) must have a mobile frame — SCR-25 especially, since a quotation/voucher link is routinely opened from a phone.

---

*End of Document — v1.1*
