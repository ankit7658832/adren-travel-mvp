---
id: AI-12
epic: AI Layer
phase: mock
status: not-started
story_points: 8
dependencies: ["AI-02", "BOK-12"]
labels: ["backend", "ai", "ads", "phase1"]
prd_references: ["§14.4", "§14.2"]
modules_or_screens: ["ai", "ads"]
testing_tiers: ["unit", "module (@ApplicationModuleTest)"]
---

# AI-12: Generate AI ad-creative variants grounded in package content and live pricing

## Summary (business)
This lets consultants ask the AI to generate several versions of marketing copy for a travel package automatically, saving time on advertising content. The AI is only allowed to describe the package using its real name, description, and current price, so customers never see advertising that promises something outdated or untrue.

## User Story
**As a** Consultant, **I want** have the AI generate multiple ad-creative variants for my package, grounded in its actual content and current price, **so that** PRD §14.4's AI Creative Generation requirement is met — creative must never reference stale or fabricated package details.

## Acceptance Criteria
- Given a Consultant requests creative variants for a published Package, when generation runs, then each variant's copy text is grounded in the Package's actual name/description/current sell price — never fabricated or stale.

## Developer Notes
- **PRD reference(s):** §14.4 AI Creative Generation; §14.2 step 3
- **Module(s)/Screen(s):** ai, ads
- **Story points:** 8 — Cross-module (ai↔ads) generation feeding directly into the Campaign Builder's creative gallery (ADS-04) — depends on both AI-02's generation discipline and a live Package read.
- **Dependencies:** AI-02, BOK-12
- **Testing tier(s):** unit, module (@ApplicationModuleTest)

## Sub-tasks
- [EXTEND] Backend: ad-creative variant generation grounded in package content — Groq client call + prompt construction (`adren.ai.groq` config)
- [NEW] Backend: audit-log write as a transactional gate (backend-best-practices §7 — failed write blocks suggestion use)
- [NEW] Backend: REST endpoint
- [NEW] Backend: unit test
- [NEW] Backend: module/integration test
