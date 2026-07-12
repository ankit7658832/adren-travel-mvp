---
id: FIN-11
epic: Financial Layer
phase: mock
status: not-started
story_points: 8
dependencies: ["FIN-05"]
labels: ["backend", "financial", "payments", "phase1"]
prd_references: ["§12.4", "§24.4"]
modules_or_screens: ["payments"]
testing_tiers: ["unit", "module (@ApplicationModuleTest)", "integration (Testcontainers)"]
---

# FIN-11: Integrate Stripe for payment collection across six settlement currencies

## Summary (business)
Customers can pay for their bookings by card using Stripe, a trusted third-party payment processor, in any of six currencies (Indian Rupee, Australian Dollar, British Pound, US Dollar, UAE Dirham, or Danish Krone). Card details are captured directly by Stripe's secure system rather than passing through the company's own servers, which reduces the business's security and compliance burden around handling sensitive card data.

## User Story
**As a** User, **I want** pay for a booking via Stripe in INR, AUD, GBP, USD, AED, or DKK, **so that** PRD §12.4 and §24.4's PCI-minimization NFR (hosted elements, no raw card data server-side) are both satisfied.

## Acceptance Criteria
- Given a User selects Stripe as the payment method for a GBP booking, when they submit payment, then Stripe's hosted payment element handles card capture — no raw PAN ever reaches the Adren backend.
- Given payment succeeds, when the webhook is received, then the booking confirms and the wallet/ledger (if applicable) reconciles.

## Developer Notes
- **PRD reference(s):** §12.4 Stripe Integration; §24.4 NFR (PCI-DSS scope)
- **Module(s)/Screen(s):** payments
- **Story points:** 8 — Multi-currency Stripe wiring plus webhook handling — the most externally-integrated story in this epic.
- **Dependencies:** FIN-05
- **Testing tier(s):** unit, module (@ApplicationModuleTest), integration (Testcontainers)

## Sub-tasks
- [NEW] Backend: Stripe PaymentIntent creation per currency + webhook handler
- [NEW] Backend: booking confirmation gated on webhook receipt
- [NEW] Backend: unit test — PaymentIntent request shape per currency
- [NEW] Backend: integrationTest — webhook-driven confirmation flow
