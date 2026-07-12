---
id: AI-06
epic: AI Layer
phase: mock
status: not-started
story_points: 5
dependencies: ["AI-02", "BOK-08"]
labels: ["backend", "ai", "phase1"]
prd_references: ["§11.2", "§6"]
modules_or_screens: ["ai", "booking"]
testing_tiers: ["unit", "module (@ApplicationModuleTest)"]
---

# AI-06: Enforce mandatory human-in-the-loop approval before an AI itinerary reaches the traveler

## Summary (business)
No AI-generated itinerary can be sent to a customer until a human consultant has explicitly reviewed and approved it. This ensures a person is always accountable for what travelers see, preventing AI mistakes from reaching customers unchecked.

## User Story
**As a** Consultant/User, **I want** have every AI-generated itinerary require my explicit approval before it can be shared with a traveler, **so that** PRD §11.2 principle 3 is enforced at the workflow level, not left to convention.

## Acceptance Criteria
- Given an AI-generated itinerary is produced, when an attempt is made to save it as a Quotation or share it with a traveler without approval, then the workflow blocks the transition until the Consultant/permitted User explicitly approves.

## Developer Notes
- **PRD reference(s):** §11.2 principle 3; §6 Roles & Permissions Matrix (AI approval row)
- **Module(s)/Screen(s):** ai, booking
- **Story points:** 5 — Workflow gate wired into BOK-08's Quotation-save transition — the enforcement point matters more than the approval UI itself (AI-10 covers the UI).
- **Dependencies:** AI-02, BOK-08
- **Testing tier(s):** unit, module (@ApplicationModuleTest)

## Sub-tasks
- [EXTEND] Backend: `requireAiApproval` business logic / state-transition method
- [EXTEND] Backend: domain event publication (`@Transactional`, same method scope)
- [EXTEND] Backend: REST endpoint `(internal — gate on Itinerary.markAsQuotation for AI-generated itineraries)`
- [NEW] Backend: unit test
- [NEW] Backend: module/integration test
