---
id: SEC-07
epic: Security Hardening
phase: production
status: not-started
story_points: 5
dependencies: ["FIN-11"]
labels: ["backend", "security", "payments", "phase2"]
prd_references: ["§24.4"]
modules_or_screens: ["payments"]
testing_tiers: ["integration (Testcontainers)"]
---

# SEC-07: Audit PCI-DSS scope for the Stripe hosted-elements integration

## Summary (business)
This story brings in an independent auditor to confirm that Adren's backend systems never see or store customers' raw credit card numbers or security codes, since all card entry is handled directly by the payment processor (Stripe). This keeps Adren out of the most burdensome and costly category of payment-security compliance requirements.

## User Story
**As a** compliance owner, **I want** have an independent audit confirm no raw card data ever reaches the Adren backend, **so that** PRD §24.4's PCI-scope-minimization NFR is externally verified, not just assumed correct from FIN-11's design.

## Acceptance Criteria
- Given the PCI-DSS scope audit runs against FIN-11's Stripe integration, when every code path is reviewed, then no raw PAN/CVV field exists in any DTO, log line, or database column — Stripe's hosted elements are confirmed as the sole card-data touchpoint.

## Developer Notes
- **PRD reference(s):** §24.4 NFR Payments & Wallet
- **Module(s)/Screen(s):** payments
- **Story points:** 5 — Audit + any remediation surfaced by it, against FIN-11's already-built hosted-elements integration.
- **Dependencies:** FIN-11
- **Testing tier(s):** integration (Testcontainers)

## Sub-tasks
- [NEW] Backend: PCI-DSS scope audit against the Stripe integration
- [NEW] Backend: remediation for any finding
- [NEW] Backend: regression test preventing raw card data from ever entering a DTO
