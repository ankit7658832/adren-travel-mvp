---
id: FIN-14
epic: Financial Layer
phase: mock
status: not-started
story_points: 3
dependencies: ["FIN-04", "FIN-13"]
labels: ["backend", "financial", "payments", "phase1"]
prd_references: ["§23.4", "§25"]
modules_or_screens: ["payments"]
testing_tiers: ["unit", "module (@ApplicationModuleTest)"]
---

# FIN-14: Reuse the original FX snapshot when calculating a refund

## Summary (business)
If a booking needs to be refunded and currency exchange rates have moved since the original purchase, the refund is calculated using the same exchange rate that was locked in at booking time, not today's rate. This ensures refunds are financially consistent and predictable, preventing the business or customer from unfairly gaining or losing money due to currency shifts.

## User Story
**As a** Consultant, **I want** have a refund calculated against the FX rate locked at booking time, not the current market rate, **so that** PRD §23.4 Edge Case #9 and T15 are closed.

## Acceptance Criteria
- Given a refund is issued in a currency different from the original booking currency due to an FX rate change between booking and cancellation, when the refund is calculated, then the amount uses the original locked `fx_rate_snapshot`, not the current rate (T15).

## Developer Notes
- **PRD reference(s):** §23.4 Edge Case #9; §25 T15
- **Module(s)/Screen(s):** payments
- **Story points:** 3 — Single, well-scoped rule reading FIN-04's immutable snapshot rather than re-fetching FX.
- **Dependencies:** FIN-04, FIN-13
- **Testing tier(s):** unit, module (@ApplicationModuleTest)

## Sub-tasks
- [EXTEND] Backend: `calculateRefund (FX reuse)` business logic / state-transition method
- [EXTEND] Backend: domain event publication (`@Transactional`, same method scope)
- [EXTEND] Backend: REST endpoint `(internal — extends FIN-13)`
- [NEW] Backend: unit test
- [NEW] Backend: module/integration test
