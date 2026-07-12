---
id: FES-03
epic: Frontend Shell
phase: mock
status: not-started
story_points: 5
dependencies: []
labels: ["frontend", "foundation", "phase1"]
prd_references: ["§7.1"]
modules_or_screens: ["Itinerary Builder (21.2)"]
testing_tiers: ["unit", "component test"]
---

# FES-03: Introduce a Zustand store for the in-progress itinerary-builder draft

## Summary (business)
This ensures that when a consultant is partway through building a multi-step travel itinerary, their in-progress work (like edits to line items) is reliably saved as they move between steps, rather than risking loss or accidental duplication. It also draws a clear line between temporary in-progress work and live data pulled from search results, so the two don't get confused and cause display errors.

## User Story
**As a** Consultant/User, **I want** have my itinerary-builder draft persist across the multi-step wizard without being lost or duplicated by React Query, **so that** RULES.md §7.1's state-management boundary is established deliberately — cross-cutting client state that outlives one component tree uses Zustand, never a copy of React Query data.

## Acceptance Criteria
- Given a Consultant swaps a line item in one step of the builder, when they navigate to another step, then the draft state persists via the Zustand store, not local `useState` scoped to the now-unmounted step component.
- Given server data (search results) is fetched, when it is inspected, then it is never copied into the Zustand store — only genuinely cross-cutting draft state lives there.

## Developer Notes
- **PRD reference(s):** §7.1 State management boundaries (RULES.md, reconciliation item)
- **Module(s)/Screen(s):** Itinerary Builder (21.2)
- **Story points:** 5 — First real Zustand usage in the codebase (currently a zero-usage dependency) — establishes the pattern the itinerary-builder and later screens will follow.
- **Dependencies:** None
- **Testing tier(s):** unit, component test

## Sub-tasks
- [NEW] Frontend: `itineraryDraftStore` (Zustand)
- [NEW] Frontend: unit test — draft persists across simulated step navigation
- [NEW] Frontend: component test — store never receives a React Query result copy
