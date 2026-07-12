---
id: FIN-04
epic: Financial Layer
phase: mock
status: not-started
story_points: 5
dependencies: ["FIN-03"]
labels: ["backend", "financial", "payments", "phase1"]
prd_references: ["§12.2", "§22.4", "§4.4"]
modules_or_screens: ["payments"]
testing_tiers: ["unit", "module (@ApplicationModuleTest)"]
---

# FIN-04: Snapshot and lock the FX rate at quotation time

## Summary (business)
When a price quote is generated, the currency exchange rate used is locked in at that moment and won't change even if currency markets move before the customer confirms the booking. This protects both the business and the customer from unexpected price changes due to currency swings between quoting and booking.

## User Story
**As a** Consultant, **I want** have the FX rate locked at the moment I generate a quote, unaffected by later market movement, **so that** PRD §12.2 and §22.4's T7 requirement are met — a booking price must use the locked snapshot, never the current rate.

## Acceptance Criteria
- Given a booking's supplier currency differs from the Consultant's sell currency, when a quote is generated, then the `fx_rate_snapshot` is locked and does not change even if market rates move before booking confirmation (T7).

## Developer Notes
- **PRD reference(s):** §12.2 FX rate snapshot; §22.4 T7; §4.4 The Money rule (RULES.md)
- **Module(s)/Screen(s):** payments
- **Story points:** 5 — Immutable-once-written value per RULES.md §4.4 — the discipline is in never re-fetching it on any downstream code path, not just writing it once.
- **Dependencies:** FIN-03
- **Testing tier(s):** unit, module (@ApplicationModuleTest)

## Sub-tasks
- [EXTEND] Backend: `snapshotFxRate` business logic / state-transition method
- [EXTEND] Backend: domain event publication (`@Transactional`, same method scope)
- [EXTEND] Backend: REST endpoint `(internal — pricing pipeline step)`
- [NEW] Backend: unit test
- [NEW] Backend: module/integration test
