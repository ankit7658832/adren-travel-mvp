---
id: OBS-04
epic: Production Observability
phase: production
status: not-started
story_points: 5
dependencies: ["CMP-01", "CMP-03", "CMP-12"]
labels: ["backend", "observability", "compliance", "phase2"]
prd_references: ["§6.3"]
modules_or_screens: ["compliance (observability)"]
testing_tiers: ["integration (Testcontainers)"]
---

# OBS-04: Provide production retention for compliance state-transition audit trails

## Summary (business)
Tax calculations, required travel-insurance disclosures, and identity-verification checks all need a permanent, searchable paper trail that outlives normal system logs. This story ensures that if a tax authority or regulator later asks why a booking was calculated or handled a certain way, the business can pull up the full record instantly, protecting the company during audits.

## User Story
**As a** compliance owner, **I want** have GST/TCS calculation inputs/outputs, ATOL disclosure completion, and KYC state changes queryable independent of app-log retention, **so that** RULES.md §6.3's compliance-audit retention requirement is honored operationally for a regulator/tax-authority query scenario.

## Acceptance Criteria
- Given a tax authority asks 'what did the system calculate and why' for a booking from 6 months prior, when a query is run against the `compliance` module's own persisted audit trail, then GST/TCS inputs/outputs, ATOL disclosure state, and KYC checklist state changes for that booking are all retrievable, independent of the app-log retention window.

## Developer Notes
- **PRD reference(s):** §6.3 (RULES.md, compliance audit)
- **Module(s)/Screen(s):** compliance (observability)
- **Story points:** 5 — Mirrors OBS-03's pattern for the `compliance` module's state-transition history (FIN-17/FIN-18/CMP-01–12).
- **Dependencies:** CMP-01, CMP-03, CMP-12
- **Testing tier(s):** integration (Testcontainers)

## Sub-tasks
- [NEW] Backend: production retention policy for compliance state-transition audit records
- [NEW] Backend: query tooling for regulator/tax-authority review
- [NEW] Backend: integrationTest
