---
id: FIN-16
epic: Financial Layer
phase: mock
status: not-started
story_points: 8
dependencies: ["FIN-13"]
labels: ["backend", "financial", "payments", "phase1"]
prd_references: ["§12.5"]
modules_or_screens: ["payments", "booking", "notification"]
testing_tiers: ["unit", "module (@ApplicationModuleTest)", "integration (Testcontainers)"]
---

# FIN-16: Build the cancellation & dispute handling workflow

## Summary (business)
Cancellations and disputes follow a clear, trackable process: the system checks the cancellation policy, works out any refund or penalty, requires a consultant's sign-off when a penalty applies, and processes the refund — while disputes are logged as trackable tickets rather than handled informally over email. This gives the business a consistent, auditable way to handle problem bookings and ensures nothing falls through the cracks.

## User Story
**As a** Consultant, **I want** have a cancellation go through policy check → refund/penalty calculation → my approval (if a penalty applies) → refund processed, and a dispute create a tracked ticket, **so that** PRD §12.5's full workflow is implemented, not just the refund-calculation piece.

## Acceptance Criteria
- Given a cancellation with an applicable penalty is submitted, when the workflow runs, then it pauses for explicit Consultant approval before the refund is processed.
- Given a dispute is flagged on a booking, when the flag is submitted, then a tracked ticket entity is created, not just an email handoff.

## Developer Notes
- **PRD reference(s):** §12.5 Cancellation & Dispute Handling
- **Module(s)/Screen(s):** payments, booking, notification
- **Story points:** 8 — Orchestrates FIN-13 across three modules (payments/booking/notification) with an explicit approval gate — the most cross-cutting Financial Layer story.
- **Dependencies:** FIN-13
- **Testing tier(s):** unit, module (@ApplicationModuleTest), integration (Testcontainers)

## Sub-tasks
- [NEW] Backend: `DisputeTicket` entity
- [EXTEND] Backend: cancellation state machine with approval gate
- [NEW] Backend: domain event on dispute creation, consumed by `notification`
- [NEW] Backend: unit test
- [NEW] Backend: integrationTest — full policy-check→approval→refund path
