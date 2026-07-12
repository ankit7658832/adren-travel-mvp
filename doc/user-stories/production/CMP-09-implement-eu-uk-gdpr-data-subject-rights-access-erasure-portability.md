---
id: CMP-09
epic: Compliance Execution
phase: production
status: not-started
story_points: 8
dependencies: ["PINF-06", "BOK-14"]
labels: ["backend", "compliance", "phase2"]
prd_references: ["§17.2", "§19"]
modules_or_screens: ["compliance"]
testing_tiers: ["integration (Testcontainers)"]
---

# CMP-09: Implement EU/UK GDPR data-subject rights (access/erasure/portability)

## Summary (business)
GDPR (a European and UK privacy law) gives individuals the right to see what personal data a company holds about them, have it deleted, or receive a copy to take elsewhere. This story builds the tools needed to fulfill these requests for EU/UK travelers — pulling together all their data on request, and deleting or anonymizing it on request (while still keeping records the law requires us to retain, such as financial documents) — so Adren can meet its legal obligations and avoid regulatory penalties.

## User Story
**As a** traveler (data subject), **I want** be able to exercise GDPR access, erasure, and portability rights on my data held by Adren, **so that** PRD §17.2's EU/UK data residency/GDPR requirement is implemented as real data-subject-rights tooling, pending PINF-06's residency decision.

## Acceptance Criteria
- Given an EU/UK traveler submits a data-access request, when it is processed, then all PII held about them across `booking`/`payments`/`ai` modules is compiled and returned within the GDPR-mandated response window.
- Given an erasure request is submitted, when it is processed, then the data is erased or anonymized per GDPR, respecting any legal-retention exceptions (e.g. financial records).

## Developer Notes
- **PRD reference(s):** §17.2 Platform Enforcement Requirements; §19 Open Items
- **Module(s)/Screen(s):** compliance
- **Story points:** 8 — Cross-module data-subject-rights tooling (booking, payments, ai all hold traveler-linked data) — broad reach, and blocked on PINF-06's residency decision for full correctness.
- **Dependencies:** PINF-06, BOK-14
- **Testing tier(s):** integration (Testcontainers)

> ⚠️ **NEEDS CLARIFICATION:** PRD §19: the EU/UK data residency approach is an open item — this story's erasure/retention logic may need rework depending on PINF-06's resolved residency decision.

## Sub-tasks
- [NEW] Backend: cross-module PII compilation for access requests
- [NEW] Backend: erasure/anonymization workflow respecting legal-retention exceptions
- [NEW] Backend: integrationTest — full access-request round trip
