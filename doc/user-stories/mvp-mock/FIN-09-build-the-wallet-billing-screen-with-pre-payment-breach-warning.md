---
id: FIN-09
epic: Financial Layer
phase: mock
status: not-started
story_points: 8
dependencies: ["FIN-08"]
labels: ["backend", "frontend", "financial", "payments", "phase1"]
prd_references: ["§21.7"]
modules_or_screens: ["payments", "Wallet & Billing Screen (21.7) — NEW feature folder"]
testing_tiers: ["unit", "module (@ApplicationModuleTest)", "component test"]
---

# FIN-09: Build the Wallet & Billing screen with pre-payment breach warning

## Summary (business)
Consultants get a dedicated screen showing their account balance and full transaction history, with an early warning if a booking they're working on would exceed their credit limit — shown before they even reach the payment step. Catching the problem early, rather than at checkout, helps consultants avoid wasted time and failed transactions.

## User Story
**As a** Consultant, **I want** see my balance, transaction ledger, and an inline warning before I reach the payment step if a pending booking would breach my credit limit, **so that** PRD §21.7's layout and breach-state requirement (warning appears before payment, not after) are both implemented.

## Acceptance Criteria
- Given a pending booking would breach the credit limit, when the User is on any screen before the payment step, then an inline warning appears before they reach payment, not after.
- Given the Consultant filters the transaction ledger by type, when they apply the filter, then only entries matching that `WalletLedgerEntry.type` (TopUp/Hold/Debit/Refund/CommissionDeduction) are shown.

## Developer Notes
- **PRD reference(s):** §21.7 Wallet & Billing Screen
- **Module(s)/Screen(s):** payments, Wallet & Billing Screen (21.7) — NEW feature folder
- **Story points:** 8 — New screen with a cross-screen breach-warning requirement (must appear before, not on, the payment step) — genuinely cross-cutting UI state.
- **Dependencies:** FIN-08
- **Testing tier(s):** unit, module (@ApplicationModuleTest), component test

## Sub-tasks
- [NEW] Backend: `GET /api/v1/wallet/ledger?type=` paginated endpoint
- [NEW] Frontend: `useWalletBilling` hook (React Query for server data per RULES.md §7.1)
- [NEW] Frontend: `WalletBilling.tsx` component — all 5 PRD Part 21 states (default/loading/success/empty/error)
- [NEW] Frontend: component test (Testing Library, co-located, asserts on role/label per RULES.md §7.3)
