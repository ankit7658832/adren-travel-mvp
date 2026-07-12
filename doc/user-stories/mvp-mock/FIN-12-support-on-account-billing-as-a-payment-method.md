---
id: FIN-12
epic: Financial Layer
phase: mock
status: not-started
story_points: 5
dependencies: ["FIN-11"]
labels: ["backend", "financial", "payments", "phase1"]
prd_references: ["§21.4", "§20.8"]
modules_or_screens: ["payments"]
testing_tiers: ["unit", "module (@ApplicationModuleTest)"]
---

# FIN-12: Support On-Account billing as a payment method

## Summary (business)
In addition to paying by card or using their wallet balance, consultants can choose to bill a booking to a company credit account to be settled later ('on-account'), giving them a third flexible payment option at checkout. This flexibility supports business relationships where a consultant needs to book now and settle payment on agreed terms afterward.

## User Story
**As a** Consultant, **I want** bill a booking to my on-account balance instead of Stripe or wallet, **so that** PRD §21.4's three payment-method options (Stripe / Wallet / On-Account) are all available at checkout.

## Acceptance Criteria
- Given a Consultant selects On-Account at checkout, when they confirm, then the booking's `payment_method` is `OnAccount` and a corresponding ledger entry is created without a Stripe call.

## Developer Notes
- **PRD reference(s):** §21.4 Booking & Payment Flow; §20.8 Booking (payment_method enum)
- **Module(s)/Screen(s):** payments
- **Story points:** 5 — Third payment-method branch reusing FIN-06's wallet/ledger machinery.
- **Dependencies:** FIN-11
- **Testing tier(s):** unit, module (@ApplicationModuleTest)

## Sub-tasks
- [EXTEND] Backend: `payOnAccount` business logic / state-transition method
- [EXTEND] Backend: domain event publication (`@Transactional`, same method scope)
- [EXTEND] Backend: REST endpoint `(internal — confirmBooking payment-method branch)`
- [NEW] Backend: unit test
- [NEW] Backend: module/integration test
