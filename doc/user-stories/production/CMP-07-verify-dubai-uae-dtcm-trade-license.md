---
id: CMP-07
epic: Compliance Execution
phase: production
status: not-started
story_points: 5
dependencies: ["FND-04"]
labels: ["backend", "compliance", "phase2"]
prd_references: ["§17.1", "§13.1"]
modules_or_screens: ["compliance"]
testing_tiers: ["unit", "module (@ApplicationModuleTest)"]
---

# CMP-07: Verify Dubai/UAE DTCM trade license

## Summary (business)
In Dubai/UAE, travel businesses must hold a trade license issued by DTCM (the Dubai government's tourism authority). This story adds a real verification step so the platform checks that a consultant's license has actually been confirmed valid before letting them perform licensed activities, instead of just taking their word for it at signup.

## User Story
**As a** Consultant, **I want** have my DTCM trade license verified as part of onboarding enforcement, **so that** PRD §17.1's Dubai/UAE row moves from capture-only to a real verification workflow.

## Acceptance Criteria
- Given a Dubai/UAE-based Consultant's DTCM license is not yet verified, when they attempt a licensed action, then the platform blocks or flags the action per the confirmed verification workflow.

## Developer Notes
- **PRD reference(s):** §17.1 Market-by-Market Requirements (Dubai/UAE); §13.1 Consultant Onboarding
- **Module(s)/Screen(s):** compliance
- **Story points:** 5 — Same enforcement-layer pattern as CMP-05, applied to the DTCM license.
- **Dependencies:** FND-04
- **Testing tier(s):** unit, module (@ApplicationModuleTest)

## Sub-tasks
- [NEW] Backend: `enforceDtcmVerification` business logic / state-transition method
- [NEW] Backend: domain event publication (`@Transactional`, same method scope)
- [NEW] Backend: REST endpoint `(internal — action-gate for Dubai/UAE-market Consultants)`
- [NEW] Backend: unit test
- [NEW] Backend: module/integration test
