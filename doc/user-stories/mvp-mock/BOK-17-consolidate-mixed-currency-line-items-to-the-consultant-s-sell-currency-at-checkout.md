---
id: BOK-17
epic: Booking Core
phase: mock
status: not-started
story_points: 5
dependencies: ["BOK-13", "FIN-04"]
labels: ["backend", "booking", "phase1"]
prd_references: ["§23.1"]
modules_or_screens: ["booking", "payments"]
testing_tiers: ["unit", "module (@ApplicationModuleTest)"]
---

# BOK-17: Consolidate mixed-currency line items to the Consultant's sell currency at checkout

## Summary (business)
If a trip combines items priced in different currencies (for example, because of how a particular local supplier is set up), the customer only ever sees one final total in the Consultant's own selling currency, never a confusing mix of currencies. This guarantees pricing clarity at checkout regardless of how many different suppliers and currencies are behind the scenes.

## User Story
**As a** Consultant, **I want** see one consolidated total in my sell currency even if a BYOS supplier line item is priced in a different currency, **so that** PRD §23.1 Edge Case #2 is closed — the system must never present a mixed-currency total.

## Acceptance Criteria
- Given an itinerary contains a mix of INR and AED line items due to a differently-configured BYOS supplier, when checkout is reached, then the system consolidates to the Consultant's sell currency using the FX layer, never presenting a mixed-currency total.

## Developer Notes
- **PRD reference(s):** §23.1 Edge Case #2
- **Module(s)/Screen(s):** booking, payments
- **Story points:** 5 — Depends on FIN-04's FX-snapshot mechanism; the booking-side work is the checkout-time consolidation rule itself.
- **Dependencies:** BOK-13, FIN-04
- **Testing tier(s):** unit, module (@ApplicationModuleTest)

## Sub-tasks
- [EXTEND] Backend: `consolidateCheckoutCurrency` business logic / state-transition method
- [EXTEND] Backend: domain event publication (`@Transactional`, same method scope)
- [EXTEND] Backend: REST endpoint `(internal — invoked during confirmBooking)`
- [NEW] Backend: unit test
- [NEW] Backend: module/integration test
