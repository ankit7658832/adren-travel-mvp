---
id: FIN-13
epic: Financial Layer
phase: mock
status: not-started
story_points: 5
dependencies: ["FIN-11", "BOK-03"]
labels: ["backend", "financial", "payments", "phase1"]
prd_references: ["§12.4", "§12.5"]
modules_or_screens: ["payments", "booking"]
testing_tiers: ["unit", "module (@ApplicationModuleTest)"]
---

# FIN-13: Process refunds and credit notes tied to supplier cancellation policy

## Summary (business)
When a customer cancels a booking, any refund or penalty is calculated based on the actual cancellation terms set by the original travel supplier (e.g. hotel, airline), rather than a generic flat rule, and a consultant must approve the outcome before money moves if a penalty applies. This ensures refunds are fair, accurate, and aligned with real supplier contracts, protecting the business from over- or under-refunding customers.

## User Story
**As a** Consultant, **I want** have a cancellation's refund or penalty calculated against the actual supplier cancellation policy, **so that** PRD §12.4/§12.5's refund workflow reflects real policy terms rather than a flat rule.

## Acceptance Criteria
- Given a booking is cancelled before its cancellation deadline, when the refund is calculated, then it reflects the supplier's cancellation policy captured on the line item (e.g. `cancellation_deadline`) rather than a flat percentage.
- Given a penalty applies, when the refund is calculated, then Consultant approval is required before the refund is processed.

## Developer Notes
- **PRD reference(s):** §12.4 Stripe Integration (refund/credit-note); §12.5 Cancellation & Dispute Handling
- **Module(s)/Screen(s):** payments, booking
- **Story points:** 5 — Reads existing line-item cancellation-policy fields; the calculation branches by policy shape.
- **Dependencies:** FIN-11, BOK-03
- **Testing tier(s):** unit, module (@ApplicationModuleTest)

## Sub-tasks
- [NEW] Backend: `calculateRefund` business logic / state-transition method
- [NEW] Backend: domain event publication (`@Transactional`, same method scope)
- [NEW] Backend: REST endpoint `POST /api/v1/bookings/{id}/cancellation`
- [NEW] Backend: unit test
- [NEW] Backend: module/integration test
