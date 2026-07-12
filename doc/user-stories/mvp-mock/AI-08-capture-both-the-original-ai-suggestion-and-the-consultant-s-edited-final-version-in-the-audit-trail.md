---
id: AI-08
epic: AI Layer
phase: mock
status: not-started
story_points: 5
dependencies: ["AI-07"]
labels: ["backend", "ai", "phase1"]
prd_references: ["§23.3", "§25"]
modules_or_screens: ["ai"]
testing_tiers: ["unit", "module (@ApplicationModuleTest)"]
---

# AI-08: Capture both the original AI suggestion and the Consultant's edited final version in the audit trail

## Summary (business)
When a consultant edits an AI-generated trip suggestion before approving it, the system keeps a record of both the AI's original suggestion and the consultant's final edited version, rather than replacing one with the other. This lets the business see clearly how much human judgment was applied on top of AI output, which is important for accountability and quality review.

## User Story
**As a** Super Admin, **I want** see both what the AI originally suggested and what the Consultant changed it to before approval, **so that** PRD §23.3 Edge Case #8 and T14 are satisfied — the audit log must never overwrite the original.

## Acceptance Criteria
- Given an AI-suggested itinerary is edited by the Consultant after generation, then re-approved, when the audit log is inspected, then both the original AI output and the edited final version are present, not overwritten (T14).

## Developer Notes
- **PRD reference(s):** §23.3 Edge Case #8; §25 T14
- **Module(s)/Screen(s):** ai
- **Story points:** 5 — Extends AI-07's audit entity with a linked edit-history record.
- **Dependencies:** AI-07
- **Testing tier(s):** unit, module (@ApplicationModuleTest)

## Sub-tasks
- [EXTEND] Backend: `AiSuggestionAuditLog` gains a linked `editedFinalVersion` record on approval
- [NEW] Backend: unit test — original preserved after edit
- [NEW] Backend: module test
