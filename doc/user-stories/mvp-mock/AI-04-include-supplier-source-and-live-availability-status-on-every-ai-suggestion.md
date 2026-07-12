---
id: AI-04
epic: AI Layer
phase: mock
status: not-started
story_points: 5
dependencies: ["AI-02"]
labels: ["backend", "ai", "phase1"]
prd_references: ["§11.3", "§11.2"]
modules_or_screens: ["ai"]
testing_tiers: ["unit", "module (@ApplicationModuleTest)"]
---

# AI-04: Include supplier source and live-availability status on every AI suggestion

## Summary (business)
Every AI-suggested trip item will clearly show which travel supplier it came from and how recently its availability was confirmed, before the consultant approves it. This gives consultants the confidence and transparency needed to trust and verify AI suggestions rather than approving them blindly.

## User Story
**As a** Consultant/User, **I want** see which supplier and how current the availability is for every AI-suggested line item before approving it, **so that** PRD §11.3's acceptance criterion — every line item shows supplier source and live status before approval — is met as a first-class response field, not something the frontend has to infer.

## Acceptance Criteria
- Given an AI-generated itinerary is produced, when each line item is inspected, then it carries `supplierId` and an availability-as-of timestamp as explicit response fields.

## Developer Notes
- **PRD reference(s):** §11.3 Acceptance Criteria; §11.2 principle 2 (confidence & availability indicators)
- **Module(s)/Screen(s):** ai
- **Story points:** 5 — DTO-shape discipline on top of AI-02/03's response — backend-best-practices §7 calls this out explicitly as a first-class-field requirement.
- **Dependencies:** AI-02
- **Testing tier(s):** unit, module (@ApplicationModuleTest)

## Sub-tasks
- [EXTEND] Backend: `AiSuggestionResponse (supplierId + availabilityAsOf fields)` business logic / state-transition method
- [EXTEND] Backend: domain event publication (`@Transactional`, same method scope)
- [EXTEND] Backend: REST endpoint `(internal — response DTO shape)`
- [NEW] Backend: unit test
- [NEW] Backend: module/integration test
