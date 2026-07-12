---
id: FND-18
epic: Foundation
phase: mock
status: not-started
story_points: 3
dependencies: ["FES-01"]
labels: ["frontend", "foundation", "phase1"]
prd_references: ["§7.4"]
modules_or_screens: ["Frontend shell (all screens)"]
testing_tiers: ["component test"]
---

# FND-18: Add a root and per-route ErrorBoundary

## Summary (business)
This story ensures that if something goes wrong on one part of the website, the user sees a friendly "something went wrong" message instead of a blank broken screen, and the rest of the site keeps working normally. This prevents a small glitch in one feature from disrupting a consultant's in-progress booking elsewhere on the site.

## User Story
**As a** Consultant/User, **I want** see a graceful fallback instead of a blank white screen if any part of the app crashes, **so that** one feature's bug (e.g. the not-yet-built itinerary-builder) never takes down navigation or an in-progress booking on another screen, per RULES.md §7.4.

## Acceptance Criteria
- Given any component throws during render, when React unwinds to the nearest boundary, then a root-level boundary renders a generic 'something went wrong, reload' fallback as the last line of defense.
- Given a component inside one routed feature throws, when the error propagates, then only that route's boundary catches it — navigation and other in-progress screens remain usable.

## Developer Notes
- **PRD reference(s):** §7.4 Error boundary strategy (RULES.md, reconciliation item #6)
- **Module(s)/Screen(s):** Frontend shell (all screens)
- **Story points:** 3 — Well-scoped per RULES.md's explicit spec; react-error-boundary library adoption plus wiring at two levels.
- **Dependencies:** FES-01
- **Testing tier(s):** component test

## Sub-tasks
- [NEW] Frontend: root `ErrorBoundary` wrapping the router in `main.tsx`
- [NEW] Frontend: per-route boundary wrapping each `<Route element={...}>`
- [NEW] Frontend: `useQueryErrorResetBoundary` wiring for query-originated failures
- [NEW] Frontend: component test — simulated throw at each boundary level
