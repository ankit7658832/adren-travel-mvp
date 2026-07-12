---
id: HRD-08
epic: Hardening
phase: mock
status: not-started
story_points: 5
dependencies: ["HRD-07"]
labels: ["frontend", "booking", "phase1"]
prd_references: ["§21.9"]
modules_or_screens: ["booking", "PNR / Booking Search (21.9) — NEW feature folder"]
testing_tiers: ["unit", "module (@ApplicationModuleTest)", "component test", "e2e"]
---

# HRD-08: Build the PNR/Booking Search screen

## Summary (business)
This delivers the actual search screen where a staff member types in a booking reference and instantly sees a summary of that booking, with the option to click through for full details, regardless of the type of travel product involved.

## User Story
**As a** User, **I want** enter a single reference and see a booking summary across all product types, click through to full detail, **so that** PRD §21.9's layout is implemented.

## Acceptance Criteria
- Given a User submits a PNR/booking reference, when results load, then a summary is shown regardless of product type, with a click-through to the full booking detail view.

## Developer Notes
- **PRD reference(s):** §21.9 PNR / Booking Search
- **Module(s)/Screen(s):** booking, PNR / Booking Search (21.9) — NEW feature folder
- **Story points:** 5 — Frontend consumer of HRD-07's endpoint.
- **Dependencies:** HRD-07
- **Testing tier(s):** unit, module (@ApplicationModuleTest), component test, e2e

## Sub-tasks
- [NEW] Frontend: `usePnrBookingSearch` hook (React Query for server data per RULES.md §7.1)
- [NEW] Frontend: `PnrBookingSearch.tsx` component — all 5 PRD Part 21 states (default/loading/success/empty/error)
- [NEW] Frontend: component test (Testing Library, co-located, asserts on role/label per RULES.md §7.3)
- [NEW] Frontend: Playwright e2e spec (extends `search-flow.spec.ts` pattern, PRD §9.1 flow)
