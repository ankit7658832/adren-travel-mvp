---
id: FIN-15
epic: Financial Layer
phase: mock
status: not-started
story_points: 5
dependencies: ["FIN-11", "FIN-06"]
labels: ["backend", "financial", "payments", "phase1"]
prd_references: ["§23.4"]
modules_or_screens: ["payments"]
testing_tiers: ["unit", "integration (Testcontainers)"]
---

# FIN-15: Reconcile wallet top-up when the payment gateway webhook is delayed or fails

## Summary (business)
If a consultant tops up their wallet but the payment provider's confirmation is delayed or fails to arrive, the system will not let them book against those funds until the top-up is properly confirmed. This prevents consultants from spending money that hasn't actually been received yet, protecting the business from financial exposure due to payment processing delays.

## User Story
**As a** Consultant, **I want** have my wallet balance reconcile once a delayed/retried webhook is received, without being falsely allowed to book against unconfirmed funds in the interim, **so that** PRD §23.4 Edge Case #10 is closed.

## Acceptance Criteria
- Given a wallet top-up succeeds at the payment gateway but the confirming webhook fails/delays, when a booking is attempted against the not-yet-reconciled top-up, then the booking flow is blocked, not falsely allowed, until the webhook is received/retried.

## Developer Notes
- **PRD reference(s):** §23.4 Edge Case #10
- **Module(s)/Screen(s):** payments
- **Story points:** 5 — Reconciliation state machine on top of FIN-11's webhook handling and FIN-06's wallet balance.
- **Dependencies:** FIN-11, FIN-06
- **Testing tier(s):** unit, integration (Testcontainers)

## Sub-tasks
- [NEW] Backend: top-up pending/reconciled state on the wallet ledger entry
- [EXTEND] Backend: booking-eligibility check excludes pending (unreconciled) top-ups
- [NEW] Backend: unit test
- [NEW] Backend: integrationTest — delayed webhook scenario
