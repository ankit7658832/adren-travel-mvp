---
name: frontend-react-vite
description: Mechanics of adding a new screen/feature in the React + Vite frontend — feature-folder shape, PRD Part 21 screen states, routing/provider setup, path aliases. Use before creating a new src/features/ folder or touching App.tsx routing.
metadata:
  type: project-skill
---

Lean, mechanics-focused. For the *reasoning* behind these conventions — state-management boundaries (React Query vs. Zustand vs. local state), component composition standards, accessibility baseline, error-boundary strategy — see `doc/architecture/RULES.md` §7. For component-design/performance/forms conventions see `frontend-best-practices`. For test-tier mechanics see `testing-strategy`.

## Feature-folder shape

One folder per screen/feature under `src/features/`, matching PRD Part 21's screen list. `search-dashboard/` is the reference:

```
src/features/<feature-name>/
├── <Feature>.tsx              root component — orchestrates data/state, composes children
├── <Feature>.test.tsx         component test (Testing Library), co-located
├── use<Feature>.ts            extracted hook if there's non-trivial async/state logic
└── use<Feature>.test.ts       hook test, co-located
```

Shared code (API client, generic UI primitives) goes in `src/shared/`, not duplicated per feature:
```
src/shared/
├── api/          apiClient.ts — the single axios instance, baseURL /api/v1
└── components/   generic, feature-agnostic UI pieces (currently empty — first feature past search-dashboard establishes this)
```

## Adding a new screen

1. `mkdir src/features/<screen-name>/`.
2. Check PRD Part 21 for the screen's states — implement all four/five explicitly (default, loading, success, empty, error). `SearchDashboard.tsx` is the template: `role="status"` for loading, `role="alert"` for errors, explicit "No inventory available" branch rather than silently omitting empty results.
3. Add the route in `App.tsx` inside `<Routes>`. Wrap it in an error boundary per `RULES.md` §7.4 (not yet established anywhere in the codebase — first feature to add one sets the pattern).
4. Write the component test alongside it (Testing Library, asserting on user-visible behavior — `getByRole`/`getByLabelText`, not implementation details) before wiring up the real API call.
5. If the screen needs server data, use React Query (`useQuery`/`useMutation` against `shared/api/apiClient`) — see `RULES.md` §7.1 for the state-management boundary before reaching for Zustand or local state.

## Provider stack (`src/main.tsx`)

Current order, outer→inner: `React.StrictMode` → `QueryClientProvider` (retry: 1, staleTime: 30_000) → `BrowserRouter` → `App`. Any new app-wide provider (theme, auth context) slots in between `QueryClientProvider` and `BrowserRouter` unless it specifically needs router context. No `ErrorBoundary` wraps the tree yet — see `RULES.md` §7.4 reconciliation item.

## Path alias status

`tsconfig.json` declares `"@/*": ["src/*"]` but `vite.config.ts` has no matching `resolve.alias`, and nothing uses it yet — this is a known half-configured item, see `RULES.md` §7.5. Until it's resolved one way or the other, use relative imports (`./useMultiLocationSearch`) matching the existing code, don't introduce `@/`-prefixed imports that won't resolve at build time.

## Running locally

```bash
npm install
npm run dev            # http://localhost:5173, proxies /api to backend:8080 (see vite.config.ts)
```

## New-feature checklist

- [ ] Folder under `src/features/<name>/`, root component + co-located test.
- [ ] All PRD Part 21 states for this screen implemented, not just happy path.
- [ ] Route added in `App.tsx`, wrapped in an error boundary.
- [ ] Server data via React Query, not local `useState` + manual fetch (`RULES.md` §7.1).
- [ ] Every prop/hook-return shape with >1 field has a named `interface`/`type`.
