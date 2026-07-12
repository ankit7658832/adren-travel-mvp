---
id: FND-13
epic: Foundation
phase: mock
status: not-started
story_points: 8
dependencies: ["FND-01"]
labels: ["backend", "frontend", "foundation", "booking", "phase1"]
prd_references: ["§9.1", "§21.1", "§22.1"]
modules_or_screens: ["booking", "supplier", "Search Dashboard (21.1) — EXISTING reference"]
testing_tiers: ["unit", "module (@ApplicationModuleTest)", "component test", "e2e"]
---

# FND-13: Extend Search Dashboard with map-based multi-location, multi-select search

## Summary (business)
This story upgrades the search experience so a consultant can type in several destinations at once and see them all plotted on a map, making it faster to start planning a multi-stop trip. This reduces the manual, repetitive work of searching one location at a time.

## User Story
**As a** Consultant/User, **I want** enter multiple locations in a single search box and see them geocoded on a map, **so that** I can start building a multi-location itinerary in one search step, per PRD §9.1 Flow A steps 2–4.

## Acceptance Criteria
- Given a Consultant enters 3+ locations in the search box, when they submit the search, then the system geocodes and displays a map pin for every location, even one with no inventory (T1).
- Given a search is in progress, when the Consultant edits the search box, then the in-progress search is cancelled rather than merging both result sets.

## Developer Notes
- **PRD reference(s):** §9.1 Flow A (steps 1-4); §21.1 Search Dashboard; §22.1 Multi-Location Search
- **Module(s)/Screen(s):** booking, supplier, Search Dashboard (21.1) — EXISTING reference
- **Story points:** 8 — Extends the existing `search-dashboard` reference feature with real geocoding + a map panel — the current implementation is a mocked, single-shot hook per RULES.md §7.1's reconcile note.
- **Dependencies:** FND-01
- **Testing tier(s):** unit, module (@ApplicationModuleTest), component test, e2e

## Sub-tasks
- [NEW] Backend: `geocodeAndSearch` business logic / state-transition method
- [NEW] Backend: domain event publication (`@Transactional`, same method scope)
- [NEW] Backend: REST endpoint `POST /api/v1/search`
- [NEW] Backend: unit test
- [NEW] Backend: module/integration test
- [EXTEND] Frontend: `useMultiLocationSearch` hook (React Query for server data per RULES.md §7.1)
- [EXTEND] Frontend: `SearchDashboard.tsx` component — all 5 PRD Part 21 states (default/loading/success/empty/error)
- [NEW] Frontend: component test (Testing Library, co-located, asserts on role/label per RULES.md §7.3)
- [NEW] Frontend: Playwright e2e spec (extends `search-flow.spec.ts` pattern, PRD §9.1 flow)
- [EXTEND] Frontend: `MapPanel` shared component wired to geocoded pins
