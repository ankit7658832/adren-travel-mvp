---
id: FIN-03
epic: Financial Layer
phase: mock
status: not-started
story_points: 3
dependencies: ["FIN-01"]
labels: ["backend", "financial", "payments", "phase1"]
prd_references: ["§12.2", "§12.1"]
modules_or_screens: ["payments"]
testing_tiers: ["unit", "module (@ApplicationModuleTest)"]
---

# FIN-03: Apply a configurable currency buffer on top of markup

## Summary (business)
When a booking involves a foreign currency, the system can add a small protective buffer (2-5%) on top of the consultant's margin, which can be tailored per consultant or market. This cushions the business against losses caused by exchange rate fluctuations between the time a price is quoted and when the booking is actually paid for.

## User Story
**As a** Consultant, **I want** have a 2–5% currency buffer applied above my markup, configurable per Consultant/market, **so that** FX exposure on multi-currency bookings is absorbed per PRD §12.2 and Worked Example B.

## Acceptance Criteria
- Given a Consultant has a 3% currency buffer configured on a Hotelbeds EUR rate, when a hotel line item is added, then the buffer is applied to the FX-converted base before markup, matching Worked Example B's INR 9,600 → INR 9,888 step.

## Developer Notes
- **PRD reference(s):** §12.2 Multi-Currency & FX Buffer; §12.1 Worked Example B
- **Module(s)/Screen(s):** payments
- **Story points:** 3 — Single configurable percentage applied at a defined point in the pricing pipeline.
- **Dependencies:** FIN-01
- **Testing tier(s):** unit, module (@ApplicationModuleTest)

## Sub-tasks
- [EXTEND] Backend: `applyCurrencyBuffer` business logic / state-transition method
- [EXTEND] Backend: domain event publication (`@Transactional`, same method scope)
- [EXTEND] Backend: REST endpoint `(internal — pricing pipeline step)`
- [NEW] Backend: unit test
- [NEW] Backend: module/integration test
