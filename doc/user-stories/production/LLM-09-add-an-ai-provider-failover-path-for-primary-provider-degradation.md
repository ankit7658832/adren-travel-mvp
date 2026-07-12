---
id: LLM-09
epic: LLM Production Readiness
phase: production
status: not-started
story_points: 8
dependencies: ["LLM-02", "LLM-05"]
labels: ["backend", "ai", "phase2"]
prd_references: ["§11.2"]
modules_or_screens: ["ai"]
testing_tiers: ["unit", "module (@ApplicationModuleTest)", "integration (Testcontainers)"]
---

# LLM-09: Add an AI provider failover path for primary-provider degradation

## Summary (business)
If our main AI provider starts having problems or going down, the system will automatically switch to a backup provider so Consultants and customers still get a suggestion, or a clear message that none is available. This prevents the booking experience from freezing or hanging if one AI vendor has an outage.

## User Story
**As a** Consultant/User, **I want** still get an AI suggestion (or an explicit failure state) if the primary LLM provider is degraded, **so that** LLM-02's provider abstraction is used for resilience, not just swap-at-deploy-time flexibility.

## Acceptance Criteria
- Given the primary LLM provider is degraded (elevated error rate/timeout), when an AI request is made, then the system fails over to a configured secondary `LlmProvider` implementation, or returns AI-05's explicit `NoViableSuggestion` state if no failover is configured — never a silent hang.

## Developer Notes
- **PRD reference(s):** §11.2 Governance Framework (resilience)
- **Module(s)/Screen(s):** ai
- **Story points:** 8 — Runtime failover logic (not just swap-at-deploy) built on LLM-02's abstraction — the most operationally complex LLM story.
- **Dependencies:** LLM-02, LLM-05
- **Testing tier(s):** unit, module (@ApplicationModuleTest), integration (Testcontainers)

## Sub-tasks
- [EXTEND] Backend: `LlmProvider` failover chain with health-based routing
- [NEW] Backend: unit test — failover triggers on simulated degradation
- [NEW] Backend: integrationTest — end-to-end failover path
