---
id: BOK-12
epic: Booking Core
phase: mock
status: not-started
story_points: 3
dependencies: ["BOK-11"]
labels: ["backend", "booking", "phase1"]
prd_references: ["§9.1", "§22.3"]
modules_or_screens: ["booking"]
testing_tiers: ["unit", "module (@ApplicationModuleTest)"]
---

# BOK-12: Publish a Package, making it visible to Users

## Summary (business)
Once a Package is finalized, a Consultant can publish it so their customers can find and book it, and can optionally choose to promote it through paid advertising campaigns. This is how curated trip packages actually reach paying customers and, optionally, get extra visibility through marketing.

## User Story
**As a** Consultant, **I want** publish a Package so it becomes visible to my Users and eligible for Meta campaign promotion, **so that** Users can search/sell it and I can opt into the Ads flow, per PRD §9.1 Flow B step 3 and §22.3.

## Acceptance Criteria
- Given a Quotation is converted to a Package, when the Package is published, then it becomes visible to the Consultant's Users (T4-adjacent lifecycle check).
- Given the Consultant opts into 'Promote this Package', when publish completes, then the flow hands off into the Ads Campaign Builder (ADS-03).

## Developer Notes
- **PRD reference(s):** §9.1 Flow B step 3; §22.3 (Package published → visible to Users)
- **Module(s)/Screen(s):** booking
- **Story points:** 3 — Status-transition method plus a visibility query, on top of BOK-11's gate.
- **Dependencies:** BOK-11
- **Testing tier(s):** unit, module (@ApplicationModuleTest)

## Sub-tasks
- [EXTEND] Backend: `Package.publish()` business logic / state-transition method
- [EXTEND] Backend: domain event publication (`@Transactional`, same method scope)
- [EXTEND] Backend: REST endpoint `POST /api/v1/packages/{id}/publish`
- [NEW] Backend: unit test
- [NEW] Backend: module/integration test
