---
id: CMP-06
epic: Compliance Execution
phase: production
status: not-started
story_points: 8
dependencies: ["FND-04"]
labels: ["backend", "compliance", "phase2"]
prd_references: ["§17.1", "§22.9", "§25"]
modules_or_screens: ["compliance"]
testing_tiers: ["unit", "module (@ApplicationModuleTest)"]
---

# CMP-06: Capture and enforce USA state-level Seller of Travel registration

## Summary (business)
In the United States, several states (including California, Florida, Washington, Hawaii, and Iowa) legally require travel sellers to register as a "Seller of Travel" before doing business there. Currently we only note this requirement during signup without enforcing it. This story ensures the platform actually blocks state-specific selling activity until a consultant's required state registration is confirmed, reducing legal risk for both Adren and its consultants.

## User Story
**As a** Consultant, **I want** have my state-level Seller of Travel registration (CA/FL/WA/HI/IA) captured and enforced, **so that** PRD §17.1's USA row and §22.9's T18 acceptance criterion are fully implemented, not just flagged at onboarding.

## Acceptance Criteria
- Given a Consultant's declared home market is USA and their declared state is California, when they complete onboarding, then the system flags the California Seller of Travel registration requirement (T18), and blocks state-specific actions until registration is confirmed.

## Developer Notes
- **PRD reference(s):** §17.1 Market-by-Market Requirements (USA); §22.9 T18; §25 T18
- **Module(s)/Screen(s):** compliance
- **Story points:** 8 — Five-state-specific rule set (CA/FL/WA/HI/IA), each potentially with distinct requirements — broader than the single-rule market stories in this epic.
- **Dependencies:** FND-04
- **Testing tier(s):** unit, module (@ApplicationModuleTest)

## Sub-tasks
- [NEW] Backend: `enforceStateSellerOfTravel` business logic / state-transition method
- [NEW] Backend: domain event publication (`@Transactional`, same method scope)
- [NEW] Backend: REST endpoint `(internal — per-state rule table, data-driven per §24.7)`
- [NEW] Backend: unit test
- [NEW] Backend: module/integration test
