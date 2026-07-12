---
id: FIN-10
epic: Financial Layer
phase: mock
status: not-started
story_points: 5
dependencies: ["FIN-07"]
labels: ["backend", "financial", "payments", "phase1"]
prd_references: ["§24.4", "§4.3"]
modules_or_screens: ["payments"]
testing_tiers: ["unit", "integration (Testcontainers)"]
---

# FIN-10: Guarantee atomic, idempotent wallet ledger writes

## Summary (business)
Every financial transaction recorded against a consultant's account is designed to complete fully or not at all, and if the same request is accidentally sent twice (for example, due to a network hiccup), the system won't charge the consultant twice. This is a core financial safeguard that protects both the business and consultants from accounting errors or duplicate charges.

## User Story
**As a** platform reliability owner, **I want** have every wallet ledger write be atomic and safe to retry without a double-debit, **so that** PRD §24.4's NFR is met, matching RULES.md §4.3's transaction-boundary discipline.

## Acceptance Criteria
- Given a wallet debit request is retried after a network timeout on the first attempt that actually succeeded server-side, when the retry is processed, then no second debit occurs — the retry is a no-op against the same idempotency key.

## Developer Notes
- **PRD reference(s):** §24.4 NFR Payments & Wallet; §4.3 Transaction boundaries (RULES.md)
- **Module(s)/Screen(s):** payments
- **Story points:** 5 — Idempotency-key pattern layered onto FIN-07's ledger writes, per RULES.md §2.2's dedup-key guidance.
- **Dependencies:** FIN-07
- **Testing tier(s):** unit, integration (Testcontainers)

## Sub-tasks
- [EXTEND] Backend: idempotency key (e.g. `(booking_id, ledger_entry_type)` unique constraint) on `WalletLedgerEntry`
- [NEW] Backend: unit test — duplicate write is a no-op
- [NEW] Backend: integrationTest — concurrent retrying writers against Testcontainers Postgres
