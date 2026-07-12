---
id: CMP-05
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

# CMP-05: Capture and enforce Australia ATAS accreditation

## Summary (business)
In Australia, travel sellers can hold an ATAS accreditation (an industry-recognized certification of trustworthiness for travel agents). We currently only record whether a consultant has this accreditation when they sign up, but don't do anything with it afterward. This story makes the platform actively check and enforce the accreditation before allowing actions that require it, protecting the business from working with unaccredited sellers where it matters.

## User Story
**As a** Consultant, **I want** have my ATAS accreditation captured at onboarding and enforced where required, **so that** PRD §17.1's Australia row and §13.1's ATAS-if-applicable KYC field move from capture-only (FND-04) to real enforcement.

## Acceptance Criteria
- Given an Australia-based Consultant's ATAS accreditation is not yet verified, when they attempt an action requiring it, then the platform blocks or flags the action per the confirmed enforcement rule, rather than only capturing the field at onboarding with no downstream check.

## Developer Notes
- **PRD reference(s):** §17.1 Market-by-Market Requirements (Australia); §13.1 Consultant Onboarding
- **Module(s)/Screen(s):** compliance
- **Story points:** 5 — Enforcement layer on top of FND-04's already-captured ATAS field.
- **Dependencies:** FND-04
- **Testing tier(s):** unit, module (@ApplicationModuleTest)

## Sub-tasks
- [NEW] Backend: `enforceAtasAccreditation` business logic / state-transition method
- [NEW] Backend: domain event publication (`@Transactional`, same method scope)
- [NEW] Backend: REST endpoint `(internal — action-gate for Australia-market Consultants)`
- [NEW] Backend: unit test
- [NEW] Backend: module/integration test
