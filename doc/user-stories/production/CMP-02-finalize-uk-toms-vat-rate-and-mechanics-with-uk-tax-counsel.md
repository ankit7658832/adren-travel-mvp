---
id: CMP-02
epic: Compliance Execution
phase: production
status: not-started
story_points: 8
dependencies: ["FIN-18"]
labels: ["backend", "compliance", "payments", "phase2"]
prd_references: ["§12.1", "§19"]
modules_or_screens: ["payments", "compliance"]
testing_tiers: ["unit", "integration (Testcontainers)"]
---

# CMP-02: Finalize UK TOMS VAT rate and mechanics with UK tax counsel

## Summary (business)
UK travel packages are taxed under a special VAT (value-added tax) scheme called TOMS, which applies to tour operators rather than standard sales tax rules. Our system currently uses a placeholder estimate for this rate; this story swaps it for the exact rate and rules confirmed by UK tax lawyers, ensuring UK bookings are taxed and invoiced correctly.

## User Story
**As a** Consultant, **I want** have UK TOMS VAT calculated using the confirmed rate and mechanics, not FIN-18's illustrative placeholder, **so that** PRD §12.1 Example D and §19's open item are formally resolved.

## Acceptance Criteria
- Given UK tax-counsel sign-off is received on the exact TOMS VAT rate and mechanics, when the config flag from FIN-18 is updated, then the calculation layer applies the confirmed rate in production, with the illustrative-rate flag removed.

## Developer Notes
- **PRD reference(s):** §12.1 Worked Example D; §19 Open Items
- **Module(s)/Screen(s):** payments, compliance
- **Story points:** 8 — Same external-dependency shape as CMP-01, for the UK market.
- **Dependencies:** FIN-18
- **Testing tier(s):** unit, integration (Testcontainers)

> ⚠️ **NEEDS CLARIFICATION:** PRD §19: exact TOMS VAT rate and mechanics require UK tax-counsel sign-off — this story cannot be finalized until that sign-off is received.

## Sub-tasks
- [EXTEND] Backend: confirmed TOMS VAT rate and mechanics replace FIN-18's illustrative config
- [NEW] Backend: unit test
- [NEW] Backend: integrationTest
