---
id: LLM-07
epic: LLM Production Readiness
phase: production
status: not-started
story_points: 5
dependencies: ["LLM-02"]
labels: ["backend", "ai", "phase2"]
prd_references: ["§24.3"]
modules_or_screens: ["ai"]
testing_tiers: ["unit", "module (@ApplicationModuleTest)"]
---

# LLM-07: Implement per-Consultant AI usage quota / budget guardrails

## Summary (business)
We will let administrators set a limit on how much AI usage (and its associated cost) each Consultant can generate, with the system warning or blocking further use once that limit is reached. This protects the company from unexpectedly high AI bills once real, unrestricted usage volumes kick in.

## User Story
**As a** Super Admin, **I want** cap the AI generation cost/volume a single Consultant can consume, **so that** production LLM cost is a real, unbounded line item once Groq/its replacement is billed on production volume, unlike the MVP's fixed dev usage.

## Acceptance Criteria
- Given a Consultant's AI usage approaches their configured quota, when a request is made, then the system either warns or blocks per the configured policy, rather than allowing unbounded per-Consultant AI cost.

## Developer Notes
- **PRD reference(s):** §24.3 NFR AI Governance (production cost concern)
- **Module(s)/Screen(s):** ai
- **Story points:** 5 — New quota-tracking mechanism; the calculation itself is straightforward, the policy configuration surface is the work.
- **Dependencies:** LLM-02
- **Testing tier(s):** unit, module (@ApplicationModuleTest)

## Sub-tasks
- [NEW] Backend: per-Consultant AI usage counter + configurable quota
- [NEW] Backend: quota-breach warn/block policy
- [NEW] Backend: unit test
- [NEW] Backend: module test
