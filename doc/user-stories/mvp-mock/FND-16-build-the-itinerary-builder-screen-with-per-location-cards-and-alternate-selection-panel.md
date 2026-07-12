---
id: FND-16
epic: Foundation
phase: mock
status: not-started
story_points: 8
dependencies: ["FND-13", "FND-14", "FES-03"]
labels: ["backend", "frontend", "foundation", "phase1"]
prd_references: ["§21.2", "§9.1"]
modules_or_screens: ["booking", "Itinerary Builder (21.2) — NEW feature folder"]
testing_tiers: ["unit", "component test", "e2e"]
---

# FND-16: Build the Itinerary Builder screen with per-location cards and alternate-selection panel

## Summary (business)
This story builds the main trip-planning screen, showing each destination's automatically chosen product with an easy way to open a panel and swap it for a different option. This gives consultants full control to accept the system's suggestions or override them before saving a customer's itinerary.

## User Story
**As a** Consultant/User, **I want** see each location's auto-selected line item and open a side panel to swap it for an alternate, **so that** I can accept defaults or override any product before saving the itinerary, per PRD §21.2.

## Acceptance Criteria
- Given a location card shows its default line item, when the Consultant clicks 'Change', then a side panel opens showing a filterable/sortable list (price, rating, supplier) of alternates for that category/location.
- Given the Consultant selects an alternate, when they confirm the change, then the line item updates and the auto-selected badge (FND-15) is removed.

## Developer Notes
- **PRD reference(s):** §21.2 Itinerary Builder; §9.1 Flow A steps 5-6
- **Module(s)/Screen(s):** booking, Itinerary Builder (21.2) — NEW feature folder
- **Story points:** 8 — New feature folder, non-trivial state (per-location, per-category selection across up to 5 product types) — first real use of the Zustand draft store from FES.
- **Dependencies:** FND-13, FND-14, FES-03
- **Testing tier(s):** unit, component test, e2e

## Sub-tasks
- [NEW] Backend: `GET /api/v1/itineraries/{id}/alternates?location=&category=` endpoint
- [NEW] Frontend: `useItineraryBuilder` hook (React Query for server data per RULES.md §7.1)
- [NEW] Frontend: `ItineraryBuilder.tsx` component — all 5 PRD Part 21 states (default/loading/success/empty/error)
- [NEW] Frontend: component test (Testing Library, co-located, asserts on role/label per RULES.md §7.3)
- [NEW] Frontend: Playwright e2e spec (extends `search-flow.spec.ts` pattern, PRD §9.1 flow)
