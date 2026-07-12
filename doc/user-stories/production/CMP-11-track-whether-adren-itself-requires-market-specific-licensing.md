---
id: CMP-11
epic: Compliance Execution
phase: production
status: not-started
story_points: 3
dependencies: []
labels: ["backend", "compliance", "phase2"]
prd_references: ["§19"]
modules_or_screens: ["compliance"]
testing_tiers: ["integration (Testcontainers)"]
---

# CMP-11: Track whether Adren itself requires market-specific licensing

## Summary (business)
Beyond making sure individual consultants are properly licensed, Adren as a company may itself need licenses or registrations to legally operate in certain countries. This story creates a formal record of whether that's the case in each of our six target markets, so any licensing requirement for the business itself is tracked and resolved before we launch there, rather than being an overlooked risk.

## User Story
**As a** Super Admin, **I want** have a documented, tracked determination of whether Adren itself (not just its Consultants) needs market-specific licensing in each of the six markets, **so that** PRD §19's open item is resolved and its operational implication (if any) is tracked, not left implicit.

## Acceptance Criteria
- Given legal counsel determines Adren itself requires licensing in a given market, when the determination is recorded, then it is tracked against that market's operational readiness, gating GA launch in that market if unresolved.

## Developer Notes
- **PRD reference(s):** §19 Open Items for Business Confirmation
- **Module(s)/Screen(s):** compliance
- **Story points:** 3 — Tracking/gating mechanism; the legal determination itself is a business process outside engineering scope.
- **Dependencies:** None
- **Testing tier(s):** integration (Testcontainers)

> ⚠️ **NEEDS CLARIFICATION:** PRD §19: whether Adren itself requires market-specific licensing is an open item for business confirmation — this story implements the tracking mechanism, not the legal determination itself.

## Sub-tasks
- [NEW] Infra: per-market Adren-licensing determination tracker, gating GA launch in that market
- [NEW] Infra: README / RULES.md documentation update
- [NEW] Infra: verification step (local run and/or CI check)
