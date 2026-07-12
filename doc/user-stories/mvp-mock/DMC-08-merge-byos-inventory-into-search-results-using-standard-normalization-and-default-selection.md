---
id: DMC-08
epic: Local DMC + BYOS
phase: mock
status: not-started
story_points: 5
dependencies: ["DMC-07", "FND-14"]
labels: ["backend", "dmc", "supplier", "phase1"]
prd_references: ["§10.4", "§22.6"]
modules_or_screens: ["supplier"]
testing_tiers: ["unit", "module (@ApplicationModuleTest)"]
---

# DMC-08: Merge BYOS inventory into search results using standard normalization and Default Selection

## Summary (business)
This ensures that inventory from a consultant's own supplier account (BYOS, "Bring Your Own Supplier") shows up in search results side-by-side with the company's standard inventory, processed and ranked using the exact same rules. Customers get a consistent, trustworthy shopping experience regardless of which supplier connection sourced the offer.

## User Story
**As a** Consultant, **I want** have my BYOS supplier's inventory appear in search results using the same normalization and default-selection logic as Adren's own suppliers, **so that** PRD §22.6's T10 acceptance criterion is met.

## Acceptance Criteria
- Given a Consultant adds their own Hotelbeds credentials via BYOS, when search runs, then BYOS inventory appears in results using the same normalization logic as Adren's own Hotelbeds connection (T10).

## Developer Notes
- **PRD reference(s):** §10.4 BYOS; §22.6 T10
- **Module(s)/Screen(s):** supplier
- **Story points:** 5 — Wires DMC-07's credential-agnostic client into `SupplierAggregationService`'s existing fan-out.
- **Dependencies:** DMC-07, FND-14
- **Testing tier(s):** unit, module (@ApplicationModuleTest)

## Sub-tasks
- [EXTEND] Backend: `BYOS fan-out inclusion` business logic / state-transition method
- [EXTEND] Backend: domain event publication (`@Transactional`, same method scope)
- [EXTEND] Backend: REST endpoint `(internal — SupplierAggregationService)`
- [NEW] Backend: unit test
- [NEW] Backend: module/integration test
