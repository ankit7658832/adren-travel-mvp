---
id: LLM-01
epic: LLM Production Readiness
phase: production
status: not-started
story_points: 5
dependencies: ["AI-01"]
labels: ["backend", "ai", "phase2"]
prd_references: ["§24.3"]
modules_or_screens: ["ai"]
testing_tiers: ["integration (Testcontainers)"]
---

# LLM-01: Run a production LLM provider bake-off against latency/cost/accuracy criteria

## Summary (business)
Before we commit long-term to Groq, the AI (artificial intelligence) engine that currently powers our trip suggestions, we will formally compare it against other providers on speed, cost, and accuracy. This ensures we're not locked into a vendor that turns out to be slower, more expensive, or less reliable than the alternatives once we're running at full scale.

## User Story
**As a** Super Admin, **I want** have a documented comparison of Groq against alternative production LLM providers before committing to Groq for GA, **so that** PRD §24.3's latency NFR and general production cost/accuracy concerns are evaluated before Groq is locked in beyond MVP.

## Acceptance Criteria
- Given the bake-off completes, when results are reviewed, then Groq and at least one alternative provider are compared on latency-per-segment, cost-per-suggestion, and grounding accuracy against a fixed evaluation set.

## Developer Notes
- **PRD reference(s):** §24.3 NFR AI Governance
- **Module(s)/Screen(s):** ai
- **Story points:** 5 — Evaluation/research deliverable with a lightweight harness — not a production code change itself.
- **Dependencies:** AI-01
- **Testing tier(s):** integration (Testcontainers)

## Sub-tasks
- [NEW] Backend: evaluation harness running the same prompt set against Groq and at least one alternative
- [NEW] Backend: latency/cost/accuracy comparison report
