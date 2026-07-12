---
id: LLM-03
epic: LLM Production Readiness
phase: production
status: not-started
story_points: 8
dependencies: ["LLM-02", "AI-05"]
labels: ["backend", "ai", "phase2"]
prd_references: ["§11.2"]
modules_or_screens: ["ai"]
testing_tiers: ["integration (Testcontainers)"]
---

# LLM-03: Run adversarial prompt-injection / grounding-bypass testing

## Summary (business)
We will deliberately try to trick the AI into inventing information it shouldn't (for example, fake pricing or availability) to make sure it always refuses to do so and instead flags that it couldn't find a reliable answer. This protects customers and the company from incorrect information appearing in real bookings.

## User Story
**As a** Super Admin, **I want** have the AI governance principles (grounded generation, no hallucination) tested against deliberate adversarial inputs, **so that** PRD §11.2's governance principles hold under attack, not just under well-formed input, before GA.

## Acceptance Criteria
- Given an adversarial prompt attempts to induce the AI to fabricate a non-supplier-confirmed line item, when the request is processed, then the grounding-only principle holds — no fabricated item is produced, and AI-05's explicit-failure-state path is triggered instead.

## Developer Notes
- **PRD reference(s):** §11.2 Governance Framework
- **Module(s)/Screen(s):** ai
- **Story points:** 8 — Adversarial test-suite construction against an LLM's actual behavior is inherently exploratory and high-effort.
- **Dependencies:** LLM-02, AI-05
- **Testing tier(s):** integration (Testcontainers)

## Sub-tasks
- [NEW] Backend: adversarial prompt test corpus targeting each of §11.2's five governance principles
- [NEW] Backend: automated regression suite run against the corpus on every `ai` module change
