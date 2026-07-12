---
id: FIN-02
epic: Financial Layer
phase: mock
status: not-started
story_points: 5
dependencies: ["FIN-01", "FIN-06"]
labels: ["backend", "financial", "payments", "phase1"]
prd_references: ["§12.1"]
modules_or_screens: ["payments"]
testing_tiers: ["unit", "module (@ApplicationModuleTest)"]
---

# FIN-02: Track Adren commission separately from Consultant markup

## Summary (business)
The system keeps the company's own commission separate from the individual consultant's profit margin on every booking, rather than lumping the two together into a single number. This gives clear, auditable visibility into who earns what from each sale, which matters for accurate payouts and financial reporting.

## User Story
**As a** Super Admin, **I want** have Adren's commission tracked separately from the Consultant's markup on every booking, **so that** PRD §12.1's Worked Example A distinction (markup vs. commission, deducted from Consultant payout) is reflected in the ledger.

## Acceptance Criteria
- Given a booking is confirmed with a 15% Consultant markup and a 5% Adren commission on net, when the ledger is inspected, then markup and commission appear as two distinct, separately-attributable amounts, not netted into one figure.

## Developer Notes
- **PRD reference(s):** §12.1 Worked Example A
- **Module(s)/Screen(s):** payments
- **Story points:** 5 — Requires a distinct ledger-entry type (`CommissionDeduction`, PRD §20.12) alongside the markup calculation from FIN-01.
- **Dependencies:** FIN-01, FIN-06
- **Testing tier(s):** unit, module (@ApplicationModuleTest)

## Sub-tasks
- [NEW] Backend: `calculateCommission` business logic / state-transition method
- [NEW] Backend: domain event publication (`@Transactional`, same method scope)
- [NEW] Backend: REST endpoint `(internal — invoked during sell-rate calculation)`
- [NEW] Backend: unit test
- [NEW] Backend: module/integration test
