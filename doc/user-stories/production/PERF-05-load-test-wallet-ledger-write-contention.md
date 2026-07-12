---
id: PERF-05
epic: Performance/Load Testing
phase: production
status: not-started
story_points: 5
dependencies: ["FIN-10"]
labels: ["backend", "performance", "phase2"]
prd_references: ["§24.4"]
modules_or_screens: ["Infra (performance test)"]
testing_tiers: ["integration (Testcontainers)"]
---

# PERF-05: Load-test wallet ledger write contention

## Summary (business)
This story verifies that when many payments or fund holds hit the same customer's wallet (a stored balance used for bookings) at the same time, the balance always ends up correct with no accidental double-charges. It protects customers' money and the company's financial accuracy under heavy simultaneous activity.

## User Story
**As a** platform reliability owner, **I want** validate FIN-10's idempotent, atomic wallet writes hold under concurrent debit/hold contention on the same wallet, **so that** PRD §24.4's atomicity/idempotency NFR holds at production concurrency, not just FIN-10's two-writer integrationTest.

## Acceptance Criteria
- Given N concurrent debit/hold requests target the same Consultant's wallet, when the load test runs, then the final balance is correct with zero double-debits at every tested concurrency level, consistent with FIN-10's idempotency guarantee.

## Developer Notes
- **PRD reference(s):** §24.4 NFR Payments & Wallet
- **Module(s)/Screen(s):** Infra (performance test)
- **Story points:** 5 — Load-test harness targeting FIN-08/FIN-10's already-built DB-constraint-backed wallet writes under higher concurrency.
- **Dependencies:** FIN-10
- **Testing tier(s):** integration (Testcontainers)

## Sub-tasks
- [NEW] Backend: concurrent wallet-write load-test scenario
- [NEW] Backend: balance-correctness assertion under load
