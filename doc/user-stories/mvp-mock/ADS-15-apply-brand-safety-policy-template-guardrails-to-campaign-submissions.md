---
id: ADS-15
epic: Ads/Campaign Management
phase: mock
status: not-started
story_points: 3
dependencies: ["ADS-06"]
labels: ["backend", "ads", "phase1"]
prd_references: ["§14.3"]
modules_or_screens: ["ads"]
testing_tiers: ["unit", "module (@ApplicationModuleTest)"]
---

# ADS-15: Apply brand-safety policy template guardrails to campaign submissions

## Summary (business)
Before a human reviewer even looks at a submitted ad, the system automatically screens the ad content against a set of brand-safety rules and flags anything that clearly breaks the rules. This speeds up the review process and reduces the manual workload on the team responsible for keeping ads compliant, without automatically rejecting anything on its own.

## User Story
**As a** Super Admin, **I want** have campaign creative checked against a brand-safety policy template before it reaches the manual policy-review queue, **so that** PRD §14.3's brand-safety review is assisted by a first-pass automated guardrail, reducing manual review load.

## Acceptance Criteria
- Given a campaign's creative variants are submitted for policy review, when the template check runs first, then obvious policy-template violations are flagged before a human reviewer sees the submission, without auto-rejecting.

## Developer Notes
- **PRD reference(s):** §14.3 Controls & Guardrails
- **Module(s)/Screen(s):** ads
- **Story points:** 3 — Rule-based template check (not AI-driven) layered ahead of ADS-06's manual review queue.
- **Dependencies:** ADS-06
- **Testing tier(s):** unit, module (@ApplicationModuleTest)

## Sub-tasks
- [NEW] Backend: brand-safety policy template rule set (data-driven)
- [EXTEND] Backend: pre-check applied ahead of the manual review queue
- [NEW] Backend: unit test
