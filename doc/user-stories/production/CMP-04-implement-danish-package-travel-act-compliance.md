---
id: CMP-04
epic: Compliance Execution
phase: production
status: not-started
story_points: 5
dependencies: ["FND-04"]
labels: ["backend", "compliance", "phase2"]
prd_references: ["§17.1"]
modules_or_screens: ["compliance"]
testing_tiers: ["unit", "module (@ApplicationModuleTest)"]
---

# CMP-04: Implement Danish Package Travel Act compliance

## Summary (business)
Denmark has its own law (the Danish Package Travel Act) that sets rules for what travel companies must disclose and how they must protect customer payments when selling package trips. This story makes the platform actually check and enforce those rules before a Danish consultant can publish a qualifying package, rather than just recording a compliance note with no real check in place.

## User Story
**As a** Consultant, **I want** have Denmark-market bookings comply with the Danish Package Travel Act, **so that** PRD §17.1's Denmark row is implemented as real platform enforcement, not just a KYC field.

## Acceptance Criteria
- Given a Denmark-based Consultant publishes a package meeting the Danish Package Travel Act's package-definition criteria, when publish is attempted, then the platform enforces the Act's applicable disclosure/protection requirements before allowing publish, mirroring the UK ATOL gate's enforcement pattern.

## Developer Notes
- **PRD reference(s):** §17.1 Market-by-Market Requirements (Denmark)
- **Module(s)/Screen(s):** compliance
- **Story points:** 5 — New market-specific compliance gate following BOK-11's established ATOL-gate pattern.
- **Dependencies:** FND-04
- **Testing tier(s):** unit, module (@ApplicationModuleTest)

## Sub-tasks
- [NEW] Backend: `enforceDanishPackageTravelAct` business logic / state-transition method
- [NEW] Backend: domain event publication (`@Transactional`, same method scope)
- [NEW] Backend: REST endpoint `(internal — publish-gate, mirrors UK ATOL pattern)`
- [NEW] Backend: unit test
- [NEW] Backend: module/integration test
