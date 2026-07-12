---
id: CMP-08
epic: Compliance Execution
phase: production
status: not-started
story_points: 5
dependencies: ["BOK-14"]
labels: ["backend", "compliance", "phase2"]
prd_references: ["§17.1"]
modules_or_screens: ["compliance"]
testing_tiers: ["unit", "module (@ApplicationModuleTest)"]
---

# CMP-08: Implement India DPDP Act data-handling compliance

## Summary (business)
India's DPDP Act (Digital Personal Data Protection Act) sets legal rules for how companies must collect, use, and protect people's personal information. This story makes sure traveler personal data from Indian bookings is handled the way the law requires — getting proper consent, using data only for its intended purpose, and honoring individuals' rights over their own data — protecting Adren from legal and regulatory risk in the Indian market.

## User Story
**As a** compliance owner, **I want** have India-market traveler PII handled per the Digital Personal Data Protection Act, **so that** PRD §17.1's India data-protection row is implemented as real data-handling controls.

## Acceptance Criteria
- Given India-market traveler PII is processed, when DPDP Act-relevant controls are evaluated, then consent capture, purpose limitation, and data-subject rights handling meet the Act's requirements.

## Developer Notes
- **PRD reference(s):** §17.1 Market-by-Market Requirements (India, Data Protection)
- **Module(s)/Screen(s):** compliance
- **Story points:** 5 — Data-handling controls scoped to the India market, parallel in shape to CMP-09's EU/UK GDPR story but a distinct legal framework.
- **Dependencies:** BOK-14
- **Testing tier(s):** unit, module (@ApplicationModuleTest)

## Sub-tasks
- [NEW] Backend: `enforceDpdpDataHandling` business logic / state-transition method
- [NEW] Backend: domain event publication (`@Transactional`, same method scope)
- [NEW] Backend: REST endpoint `(internal — India-scoped PII handling controls)`
- [NEW] Backend: unit test
- [NEW] Backend: module/integration test
