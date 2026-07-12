---
id: BOK-02
epic: Booking Core
phase: mock
status: not-started
story_points: 2
dependencies: ["BOK-01"]
labels: ["backend", "booking", "phase1"]
prd_references: ["§2.3", "§4.4"]
modules_or_screens: ["booking"]
testing_tiers: ["unit", "module (@ApplicationModuleTest)"]
---

# BOK-02: Fix BookingConfirmedEvent to carry Money instead of decomposed amount+currency

## Summary (business)
Every confirmed booking's price must always be stored together with its currency as a single, inseparable piece of information. This closes off a class of bug where a price amount could accidentally get matched with the wrong currency, which would misstate what a customer actually owes or paid.

## User Story
**As a** backend engineer, **I want** have `BookingConfirmedEvent` carry a single `Money totalSellPrice` field, **so that** nothing can pair the amount with the wrong currency downstream, matching the Money rule (RULES.md §4.4) the event currently violates.

## Acceptance Criteria
- Given `BookingConfirmedEvent` is published, when its payload is inspected, then it exposes one `Money totalSellPrice` field, not separate `BigDecimal`/`CurrencyCode` fields.

## Developer Notes
- **PRD reference(s):** §2.3 Event schema versioning (RULES.md reconcile); §4.4 The Money rule (RULES.md)
- **Module(s)/Screen(s):** booking
- **Story points:** 2 — Pre-GA fix on an event nothing yet consumes for real (notification listener is a TODO stub) — cheapest possible time to do it.
- **Dependencies:** BOK-01
- **Testing tier(s):** unit, module (@ApplicationModuleTest)

## Sub-tasks
- [EXTEND] Backend: `BookingConfirmedEvent` record signature change
- [EXTEND] Backend: `BookingServiceImpl` publish call site update
- [NEW] Backend: unit test
- [NEW] Backend: module test — event shape asserted via Modulith's `Scenario` API
