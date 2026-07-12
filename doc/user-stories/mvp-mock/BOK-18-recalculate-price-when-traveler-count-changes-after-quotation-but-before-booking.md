---
id: BOK-18
epic: Booking Core
phase: mock
status: not-started
story_points: 3
dependencies: ["BOK-09"]
labels: ["backend", "booking", "phase1"]
prd_references: ["§23.1"]
modules_or_screens: ["booking"]
testing_tiers: ["unit", "module (@ApplicationModuleTest)"]
---

# BOK-18: Recalculate price when traveler count changes after Quotation but before booking

## Summary (business)
If the number of travelers changes after a price quote was given but before the booking is finalized, the system recalculates the price fresh rather than using the old, potentially outdated quote. This prevents the business from either undercharging or overcharging customers based on stale pricing.

## User Story
**As a** Consultant, **I want** have the price recalculate, not carry over stale, if traveler count changes after a Quotation was generated, **so that** PRD §23.1 Edge Case #3 is closed.

## Acceptance Criteria
- Given traveler count changes after a Quotation is generated but before booking, when the booking is attempted, then price is recalculated from current rates, not carried over from the stale Quotation.

## Developer Notes
- **PRD reference(s):** §23.1 Edge Case #3
- **Module(s)/Screen(s):** booking
- **Story points:** 3 — Single, well-scoped business rule on an existing entity method (`Quotation.recalculate()` throwing rather than silently no-op'ing per backend-best-practices §1).
- **Dependencies:** BOK-09
- **Testing tier(s):** unit, module (@ApplicationModuleTest)

## Sub-tasks
- [EXTEND] Backend: `Quotation.recalculate()` business logic / state-transition method
- [EXTEND] Backend: domain event publication (`@Transactional`, same method scope)
- [EXTEND] Backend: REST endpoint `(internal — invoked on traveler-count change)`
- [NEW] Backend: unit test
- [NEW] Backend: module/integration test
