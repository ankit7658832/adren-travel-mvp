---
id: BOK-11
epic: Booking Core
phase: mock
status: not-started
story_points: 8
dependencies: ["BOK-10", "FND-04"]
labels: ["backend", "frontend", "booking", "phase1"]
prd_references: ["§21.3", "§17.2", "§22.3", "§20.7"]
modules_or_screens: ["booking", "compliance", "Package Builder (21.3) — NEW feature folder"]
testing_tiers: ["unit", "module (@ApplicationModuleTest)", "component test", "e2e"]
---

# BOK-11: Build the Package Builder screen with UK ATOL disclosure gate

## Summary (business)
When a Consultant builds a sellable travel Package in the UK that combines a flight and a hotel booked together, the system requires them to complete a required legal disclosure step before the Package can go live. This is a compliance safeguard: UK law requires this type of "dynamic package" to carry ATOL protection (a government-backed scheme that refunds or repatriates travelers if a travel company fails), and skipping the disclosure would expose the business to regulatory risk.

## User Story
**As a** Consultant, **I want** fill out the Package Builder form and be blocked from publishing a UK dynamic flight+hotel package until I complete the ATOL disclosure step, **so that** PRD §21.3's validation states and §17.2's UK ATOL/PTR 2018 auto-enforcement are both satisfied.

## Acceptance Criteria
- Given a Package includes both a flight and a hotel line item and the Consultant's market is UK, when the Consultant attempts to publish, then the system blocks publish until the ATOL disclosure step is completed (T5).
- Given required fields are incomplete, when the Consultant attempts to publish, then publish is blocked with field-level validation errors.

## Developer Notes
- **PRD reference(s):** §21.3 Package Builder; §17.2 Platform Enforcement; §22.3 T5; §20.7 is_dynamic_flight_hotel_combo
- **Module(s)/Screen(s):** booking, compliance, Package Builder (21.3) — NEW feature folder
- **Story points:** 8 — New screen with a non-trivial conditional gate (market + product-mix detection) — first cross-module (booking↔compliance) UI dependency in the catalogue.
- **Dependencies:** BOK-10, FND-04
- **Testing tier(s):** unit, module (@ApplicationModuleTest), component test, e2e

## Sub-tasks
- [NEW] Backend: `is_dynamic_flight_hotel_combo` detection on Package save
- [NEW] Backend: ATOL disclosure-completion gate on the publish endpoint
- [NEW] Frontend: `usePackageBuilder` hook (React Query for server data per RULES.md §7.1)
- [NEW] Frontend: `PackageBuilder.tsx` component — all 5 PRD Part 21 states (default/loading/success/empty/error)
- [NEW] Frontend: component test (Testing Library, co-located, asserts on role/label per RULES.md §7.3)
- [NEW] Frontend: Playwright e2e spec (extends `search-flow.spec.ts` pattern, PRD §9.1 flow)
