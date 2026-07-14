---
name: frontend-best-practices
description: Component design standards, performance (code-splitting, memoization discipline), error-boundary strategy, form/validation conventions, and design-system consistency rules for ADREN's React frontend. Use when writing or reviewing component/hook code, not just when scaffolding a new screen folder.
metadata:
  type: project-skill
---

This skill is about the code *inside* a feature. For feature-folder mechanics and routing see `frontend-react-vite`; for the reasoning behind state-management/accessibility/error-boundary rules see `doc/architecture/RULES.md` §7; for test-tier selection see `testing-strategy`. Assumes you've read `RULES.md` §7 — this is about writing components that satisfy those rules well, not repeating them.

## 1. Component design standards

- **Container/presentational split, applied when a feature outgrows one file** — not dogmatically from line one. `SearchDashboard.tsx` is currently a single file because the feature is small; that's fine. The signal to extract is when a component starts doing two unrelated things (fetching *and* rendering three independent visual regions), not a line-count threshold. When extracting, presentational children take typed props and own no `useQuery`/`useMutation`/Zustand calls — a presentational component that reaches into global/server state directly can't be reused or tested in isolation, which defeats the point of extracting it.
- **Every exported component's props are a named `interface`, not inline-destructured with inferred types**, once there's more than one prop — matches `RULES.md` §7.2. `interface SearchDashboardProps { ... }`, not `function Foo({ a, b, c }: { a: string; b: number; c: boolean })`. This isn't bureaucracy — a named interface is what makes a component's contract visible in an editor tooltip without opening the file, and it's the thing a future refactor diffs cleanly against.
- **Composition over configuration props.** If a component starts accepting boolean props that toggle entire sub-sections of its rendering (`showFooter`, `compactMode`, `variant: 'a' | 'b' | 'c'` growing past 2-3 values), that's a signal to split into separate composed components instead — a component whose render output is controlled by five independent booleans has 32 implicit states, most of which nobody has looked at.
- **No business logic inside JSX.** Derive values above the `return` (or in a `useMemo` if genuinely expensive — see §2), pass simple values/booleans into JSX. `search-dashboard`'s `hasInventory` check being a plain derived boolean rather than an inline ternary chain in JSX is the right shape to replicate.
- **Colocate a component with its test, its hook, and (once one exists) its styles** — the existing `search-dashboard/` folder already does this for component+hook+tests. Don't split a feature's files across a mirrored `components/`, `hooks/`, `tests/` tree at the top level; feature-folder colocation is the established pattern (see `frontend-react-vite`) and should hold inside a feature too as it grows multiple components.

## 2. Performance patterns

**Default to doing nothing.** React's default re-render behavior is fine for the vast majority of this app's components (forms, dashboards, list views at B2B-tool scale, not a real-time feed). Memoization is a targeted fix for a measured problem, not a default posture — `React.memo`/`useMemo`/`useCallback` sprinkled preemptively add cognitive overhead and dependency-array bugs without necessarily buying anything. Apply them when:

- **`useMemo`** — a derived value is genuinely expensive to compute (sorting/filtering a large list, a non-trivial calculation) *and* recomputes on every render because of an unrelated state change in the same component. Not for a simple boolean derivation or a cheap `.map()`.
- **`useCallback`** — a function is passed as a prop to a `React.memo`-wrapped child (where a new function identity every render would defeat the memoization), or is a dependency of another hook's dependency array (`useEffect`, `useMemo`) where identity stability actually matters. Not applied reflexively to every function defined inside a component.
- **`React.memo`** — a component that's expensive to render *and* re-renders often with unchanged props — most commonly a list item component inside a long map (map-pin markers, search result cards) where the parent re-renders frequently (e.g., on every keystroke of a filter input) but individual item props rarely change.

**Code-splitting**: route-level, using `React.lazy` + `Suspense` per top-level route in `App.tsx`, once there are enough distinct screens that shipping the whole app as one bundle matters (currently two routes both pointing at `SearchDashboard` — not yet a real concern, but the convention to establish as `itinerary-builder`, the Package Builder, the Super Admin Console, and the Ads/Campaign Builder land, per PRD Part 21's ten distinct screens — several of those, especially the Super Admin Console, are used by a completely different persona than the Consultant/User screens and have zero reason to be in the same initial bundle). Pair each lazy route with the error-boundary-per-route requirement in `RULES.md` §7.4 — `Suspense`'s loading fallback and the error boundary's error fallback are two different states of the same route boundary, set them up together.

**Map rendering** (multi-location search results, PRD §21.1) deserves specific attention given it's a core-flow screen: pin/marker components must be memoized once real map integration lands, since a map with N location pins re-rendering all N markers on every unrelated state change (a date-picker interaction, a filter toggle) is the most likely place in this app for a visible jank complaint — this is the concrete case §2's "list item inside a long map" guidance is anticipating, not a hypothetical.

**Avoid premature virtualization.** Don't reach for a windowing library (`react-window`, etc.) for a results list until it's actually rendering enough items to matter (dozens-to-hundreds, not the handful of per-location default selections in the current search flow) — this is a "add it when it hurts" call, not an upfront architectural decision.

## 3. Error-boundary strategy (implementation detail)

`RULES.md` §7.4 establishes *where* boundaries go (root + per-route). Implementation specifics:

- Use a class component (still the only way to implement `componentDidCatch`/`getDerivedStateFromError` — there's no hook equivalent) or a well-maintained library (`react-error-boundary`, which composes cleanly with React Query's `useQueryErrorResetBoundary` — not currently a dependency, worth adding when the first boundary is built rather than hand-rolling the reset-on-retry wiring).
- The root boundary's fallback is generic and severe ("Something went wrong — reload the page") because by definition something escaped every more specific boundary.
- Route-level boundary fallbacks should be feature-aware where cheap to make them so ("We couldn't load your search results — retry" beats a generic message), but must not themselves depend on the state that might be broken (a fallback that reads from the same Zustand store whose corruption caused the crash can itself crash — keep fallbacks dependency-light).
- Log the caught error (with the correlation ID pattern from `RULES.md` §6.1, once a frontend-side request-ID convention exists) before rendering the fallback — an error boundary that silently swallows the error and shows a nice message with no server-side visibility turns every frontend crash into a support ticket with no diagnostic trail.

## 4. Form / validation conventions

No form library is installed yet (no `react-hook-form`, no `formik`) and no validation library (no `zod`, no `yup`) — this needs to be established deliberately before the Traveler Detail form, Package Builder form, or Consultant/Super Admin onboarding wizard (PRD §21.3, §21.4, §21.6 — the onboarding wizard specifically has fields that change per selected market, per PRD §13.1's KYC table) get built, rather than each form reinventing its own state handling.

- **Recommendation: `react-hook-form` + `zod`** (via `@hookform/resolvers`) — react-hook-form's uncontrolled-input model avoids the re-render-per-keystroke cost that matters once forms get as large as the onboarding wizard or the itinerary builder's per-line-item edit panels; zod schemas can be shared between form validation and (once a backend request/response contract exists) roughly mirrored against the backend's Bean Validation rules on the same fields, keeping client/server validation logically aligned even though they're two separate implementations.
- **Validate the shape the backend will reject, not just what looks reasonable client-side** — e.g., the Money/currency rules in `RULES.md` §4.4 apply client-side too: a markup-percentage input should be constrained to reasonable bounds and decimal precision before submission, not just formatted for display, so the round-trip to the backend's Bean Validation doesn't become the first place a bad value is caught.
- **Market-dependent required fields (PRD §13.1, §21.6) are schema-driven, not a hardcoded conditional tree.** This mirrors the backend requirement in PRD §24.7 ("KYC checklist logic must be data-driven... rather than hardcoded per-market conditionals") — the frontend onboarding wizard should resolve its per-market required-field set from the same data-driven source (ideally fetched from the backend's `compliance` module once it exists, not a duplicated hardcoded map in the frontend), so a market rule change doesn't require a frontend deploy in one place and a backend deploy in another that can drift out of sync.
- **Every validation error is associated with its field via `aria-describedby`/`aria-invalid`**, not just visually adjacent red text — ties into the accessibility baseline in `RULES.md` §7.3; a screen-reader user filling out the (fairly long, multi-step) Consultant onboarding wizard needs the same "which field is wrong and why" information a sighted user gets from color/position.

## 5. Design-system consistency rules

**Decided — see `doc/DESIGN.md` and the `frontend-design-system` skill for the full architecture; this section is a pointer, not the source of truth.** Tailwind CSS + hand-built shadcn-convention primitives (CSS-variable-driven), with a hard Layer 1 (Adren chrome, fixed tokens)/Layer 2 (white-label surfaces, runtime-injected tenant tokens) split — read `frontend-design-system` before writing or reviewing any styled UI, not just when scaffolding a new screen.

- Shared primitives live in `src/shared/design-system/` (Layer 1 — `Button`, `Badge`, more as needed) and `src/shared/theming/` (Layer 2 runtime mechanism — `contrastSafety.ts`, `resolveTenantTheme.ts`, `TenantThemedSurface.tsx`, `tenantThemeStore.ts`). A component belongs in `shared/design-system` only if at least two features use it or clearly will; don't pre-build a component library speculatively ahead of real feature needs.
- **White-label theming (PRD §13.2) is fully architected — `doc/DESIGN.md` §3.** The short version for component authors: Layer 1 components (everything except the Consultant storefront) must never read a `--tenant-*` CSS variable; Layer 2 components must never render a tenant-picked color without it having passed through `resolveSafeTextColor` (`contrastSafety.ts`) first. See `frontend-design-system` for the enforceable rule set.
- Accessibility-baseline components (`RULES.md` §7.3) should be correct *once*, in `shared/design-system`, rather than re-solved per feature — wire `label`+`htmlFor`+`aria-invalid`/`aria-describedby` correctly in the shared primitive so every consumer gets it for free, versus re-deriving the a11y wiring by hand at the JSX level per feature.

## 6. Frontend checklist addendum

Beyond `RULES.md` §8's general PR checklist:

- [ ] New component's props (if >1) are a named `interface`, not inferred/inline.
- [ ] `useMemo`/`useCallback`/`React.memo` usage is justified by an actual re-render or expensive-computation concern, not applied reflexively.
- [ ] New route is code-split (`React.lazy`) once the route count justifies it, and paired with a `Suspense` fallback alongside its error boundary.
- [ ] New form uses the chosen form/validation library consistently (not a bespoke `useState`-per-field pattern) once one is adopted.
- [ ] Market-dependent form logic reads from data/config, not a hardcoded per-market conditional tree.
- [ ] New styled UI reuses `shared/components` primitives where one exists; a genuinely new generic primitive is added there, not duplicated inline in the feature.
- [ ] Any hardcoded color/spacing value in new UI is a theme token, not a literal, given per-Consultant white-label theming is a near-term requirement.
