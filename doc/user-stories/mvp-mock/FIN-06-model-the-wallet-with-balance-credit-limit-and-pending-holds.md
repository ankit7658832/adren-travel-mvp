---
id: FIN-06
epic: Financial Layer
phase: mock
status: not-started
story_points: 5
dependencies: ["FND-01"]
labels: ["backend", "financial", "payments", "phase1"]
prd_references: ["§12.3", "§20.12"]
modules_or_screens: ["payments"]
testing_tiers: ["unit", "module (@ApplicationModuleTest)"]
---

# FIN-06: Model the Wallet with balance, credit limit, and pending holds

## Summary (business)
Consultants can see their available funds, their approved credit limit, and any money currently held for pending bookings, all in one place. This single view is what the system relies on to decide whether a consultant has enough funds to confirm a new booking.

## User Story
**As a** Consultant, **I want** see my available balance, credit limit, and any pending holds in one place, **so that** PRD §12.3's wallet model is the source of truth for booking confirmation eligibility.

## Acceptance Criteria
- Given a Consultant's wallet is queried, when the response is inspected, then it exposes available balance, credit limit, and pending holds, denominated in the home-market currency.

## Developer Notes
- **PRD reference(s):** §12.3 Wallet & Credit Limit; §20.12 Wallet Ledger Entry
- **Module(s)/Screen(s):** payments
- **Story points:** 5 — New Wallet + WalletLedgerEntry entities with the enum-typed ledger from PRD §20.12.
- **Dependencies:** FND-01
- **Testing tier(s):** unit, module (@ApplicationModuleTest)

## Sub-tasks
- [NEW] Backend: `Wallet` entity + `WalletRepository` (package-private, own Flyway migration)
- [NEW] Backend: `getWallet` on the module `ServiceImpl` + domain event publication (`@Transactional`)
- [NEW] Backend: REST endpoint `GET /api/v1/wallet` (Controller depends on the module's `Api` interface only)
- [NEW] Backend: unit test (service logic, mocked repository/publisher)
- [NEW] Backend: `@ApplicationModuleTest` module-slice test (event publish/consume)
