---
id: CMP-01
epic: Compliance Execution
phase: production
status: not-started
story_points: 8
dependencies: ["FIN-17"]
labels: ["backend", "compliance", "payments", "phase2"]
prd_references: ["§12.1", "§19"]
modules_or_screens: ["payments", "compliance"]
testing_tiers: ["unit", "integration (Testcontainers)"]
---

# CMP-01: Finalize India GST/TCS rates and mechanics with tax counsel

## Summary (business)
India charges tax on travel bookings through GST (Goods and Services Tax) and TCS (Tax Collected at Source, a tax the seller must collect upfront and pass to the government). Right now our system uses estimated placeholder tax rates; this story replaces them with the exact rates and calculation rules confirmed by our tax lawyers, so invoices and government filings for Indian bookings are legally accurate rather than approximate.

## User Story
**As a** Consultant, **I want** have India GST/TCS calculated using confirmed rates and mechanics, not FIN-17's illustrative placeholder, **so that** PRD §12.1 Example C and §19's open item are formally resolved, replacing the MVP's config-flagged illustrative calculation.

## Acceptance Criteria
- Given tax-counsel sign-off is received on exact GST/TCS rates and mechanics, when the config flag from FIN-17 is updated, then the calculation layer applies the confirmed rates in production, with the illustrative-rate flag removed.

## Developer Notes
- **PRD reference(s):** §12.1 Worked Example C; §19 Open Items
- **Module(s)/Screen(s):** payments, compliance
- **Story points:** 8 — Depends entirely on external tax-counsel sign-off before the real implementation can be finalized — scope is the upper bound assuming straightforward confirmation.
- **Dependencies:** FIN-17
- **Testing tier(s):** unit, integration (Testcontainers)

> ⚠️ **NEEDS CLARIFICATION:** PRD §19: exact GST/TCS rates and mechanics require tax-counsel sign-off — this story cannot be finalized until that sign-off is received; scope/estimate may change once the real mechanics are known.

## Sub-tasks
- [EXTEND] Backend: confirmed GST/TCS rates and mechanics replace FIN-17's illustrative config
- [NEW] Backend: unit test — confirmed-rate calculation
- [NEW] Backend: integrationTest
