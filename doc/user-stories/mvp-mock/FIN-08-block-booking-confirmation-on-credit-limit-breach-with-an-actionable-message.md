---
id: FIN-08
epic: Financial Layer
phase: mock
status: not-started
story_points: 5
dependencies: ["FIN-07"]
labels: ["backend", "financial", "payments", "phase1"]
prd_references: ["§22.4", "§12.3"]
modules_or_screens: ["payments"]
testing_tiers: ["unit", "integration (Testcontainers)"]
---

# FIN-08: Block booking confirmation on credit-limit breach with an actionable message

## Summary (business)
If a consultant tries to confirm a booking that costs more than their available funds and credit allowance combined, the system stops the booking and clearly tells them they need to add funds. This protects the business from extending unapproved credit and gives consultants a clear, actionable next step instead of a confusing failure.

## User Story
**As a** Consultant, **I want** be blocked from confirming a booking that would exceed my wallet balance plus available credit, with a clear top-up prompt, **so that** PRD §22.4's T8 requirement is met.

## Acceptance Criteria
- Given a Consultant's wallet balance plus available credit is less than the booking total, when they attempt to confirm payment via wallet, then the system blocks confirmation with an actionable 'top up required' message (T8).

## Developer Notes
- **PRD reference(s):** §22.4 T8; §12.3 Wallet & Credit Limit
- **Module(s)/Screen(s):** payments
- **Story points:** 5 — Enforcement rule on top of FIN-07's hold mechanism; the DB-level constraint (backend-best-practices §3) is what makes it a real guarantee, not just an app-level check.
- **Dependencies:** FIN-07
- **Testing tier(s):** unit, integration (Testcontainers)

## Sub-tasks
- [EXTEND] Backend: conditional `UPDATE ... WHERE balance + credit_limit >= amount` with row-count check (DB-level enforcement, not app-level-only)
- [NEW] Backend: service-layer mapping to the actionable top-up message
- [NEW] Backend: unit test
- [NEW] Backend: integrationTest — breach attempt against Testcontainers Postgres
