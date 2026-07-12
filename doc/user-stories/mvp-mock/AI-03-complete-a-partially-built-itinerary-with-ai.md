---
id: AI-03
epic: AI Layer
phase: mock
status: not-started
story_points: 8
dependencies: ["AI-02"]
labels: ["backend", "ai", "phase1"]
prd_references: ["§9.1", "§21.2"]
modules_or_screens: ["ai", "booking"]
testing_tiers: ["unit", "module (@ApplicationModuleTest)"]
---

# AI-03: Complete a partially built itinerary with AI

## Summary (business)
When a consultant has already started building part of a trip, this lets them ask the AI to fill in only the missing pieces rather than starting over, speeding up the planning process. The AI respects what the consultant already chose and only adds suggestions for the gaps that remain.

## User Story
**As a** Consultant/User, **I want** click 'Complete with AI' on a partially built itinerary and have the AI fill in the remaining locations/categories, **so that** PRD §9.1 Flow A step 7 and §21.2's persistent AI-assist entry point are both satisfied.

## Acceptance Criteria
- Given an itinerary has some locations/categories already selected, when 'Complete with AI' is invoked, then the AI only proposes line items for the remaining gaps, respecting existing selections.

## Developer Notes
- **PRD reference(s):** §9.1 Flow A step 7; §21.2 Itinerary Builder (AI-assist entry point)
- **Module(s)/Screen(s):** ai, booking
- **Story points:** 8 — Same grounding/generation risk as AI-02 but scoped to partial-completion, reusing AI-02's core generation path.
- **Dependencies:** AI-02
- **Testing tier(s):** unit, module (@ApplicationModuleTest)

## Sub-tasks
- [EXTEND] Backend: partial-itinerary completion — Groq client call + prompt construction (`adren.ai.groq` config)
- [NEW] Backend: audit-log write as a transactional gate (backend-best-practices §7 — failed write blocks suggestion use)
- [NEW] Backend: REST endpoint
- [NEW] Backend: unit test
- [NEW] Backend: module/integration test
