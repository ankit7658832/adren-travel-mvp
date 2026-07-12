---
id: FIN-07
epic: Financial Layer
phase: mock
status: not-started
story_points: 5
dependencies: ["FIN-06", "BOK-13"]
labels: ["backend", "financial", "payments", "phase1"]
prd_references: ["§12.3"]
modules_or_screens: ["payments"]
testing_tiers: ["unit", "module (@ApplicationModuleTest)"]
---

# FIN-07: Place a hold on booking confirmation, release or debit on final confirmation

## Summary (business)
When a consultant starts a booking, the funds needed are temporarily set aside (held) so they can't be spent elsewhere; once the booking is confirmed, that hold becomes an actual charge, or if the booking falls through, the hold is released back to the consultant's available funds. This prevents consultants from accidentally overspending or double-committing money across multiple bookings in progress.

## User Story
**As a** Consultant, **I want** have a booking place a hold on my wallet that converts to a debit on final confirmation, or releases if the booking doesn't complete, **so that** PRD §12.3's hold lifecycle is enforced, preventing a Consultant from over-committing funds mid-booking.

## Acceptance Criteria
- Given a booking reaches the payment step with wallet selected, when a hold is placed, then the wallet's pending-holds figure increases by the booking total.
- Given the booking confirms, when the hold resolves, then it converts to a `Debit` ledger entry and pending holds decreases correspondingly.
- Given the booking is abandoned/cancelled before confirmation, when the hold resolves, then it is released back to available balance.

## Developer Notes
- **PRD reference(s):** §12.3 Wallet & Credit Limit
- **Module(s)/Screen(s):** payments
- **Story points:** 5 — State-machine on top of FIN-06's Wallet entity, invoked from BOK-13's booking flow.
- **Dependencies:** FIN-06, BOK-13
- **Testing tier(s):** unit, module (@ApplicationModuleTest)

## Sub-tasks
- [EXTEND] Backend: `placeHold/resolveHold` business logic / state-transition method
- [EXTEND] Backend: domain event publication (`@Transactional`, same method scope)
- [EXTEND] Backend: REST endpoint `(internal — invoked during confirmBooking)`
- [NEW] Backend: unit test
- [NEW] Backend: module/integration test
