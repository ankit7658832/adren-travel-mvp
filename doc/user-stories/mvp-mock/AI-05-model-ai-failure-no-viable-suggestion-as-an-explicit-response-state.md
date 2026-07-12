---
id: AI-05
epic: AI Layer
phase: mock
status: not-started
story_points: 5
dependencies: ["AI-02"]
labels: ["backend", "ai", "phase1"]
prd_references: ["§11.2", "§23.3", "§25"]
modules_or_screens: ["ai"]
testing_tiers: ["unit", "module (@ApplicationModuleTest)"]
---

# AI-05: Model AI failure/no-viable-suggestion as an explicit response state

## Summary (business)
If the AI cannot find a real trip option that fits the customer's requirements or budget, it will say so clearly instead of guessing or offering something that doesn't actually qualify. This protects customers from being shown misleading or unaffordable options and protects the business from the reputational and financial risk of overpromising.

## User Story
**As a** Consultant/User, **I want** have the AI explicitly state it cannot produce a valid suggestion rather than substituting an over-budget or fabricated option, **so that** PRD §11.2 principle 4, §23.3 Edge Case #7, and T13 are satisfied.

## Acceptance Criteria
- Given zero available suppliers exist for a location, when AI generation runs, then it states 'no inventory available' explicitly (§11.3).
- Given AI is asked to complete an itinerary with a budget no available inventory can meet, when generation runs, then it explicitly states this inability rather than silently picking the closest over-budget option (T13).

## Developer Notes
- **PRD reference(s):** §11.2 principle 4; §23.3 Edge Case #7; §25 T13
- **Module(s)/Screen(s):** ai
- **Story points:** 5 — Well-specified sealed-response-type requirement (backend-best-practices §7) — `NoViableSuggestion(reason)` as a legitimate typed value, not an exception.
- **Dependencies:** AI-02
- **Testing tier(s):** unit, module (@ApplicationModuleTest)

## Sub-tasks
- [EXTEND] Backend: `NoViableSuggestion result type` business logic / state-transition method
- [EXTEND] Backend: domain event publication (`@Transactional`, same method scope)
- [EXTEND] Backend: REST endpoint `(internal — response DTO shape)`
- [NEW] Backend: unit test
- [NEW] Backend: module/integration test
