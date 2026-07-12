---
id: PINF-07
epic: Production Infrastructure
phase: production
status: not-started
story_points: 5
dependencies: ["PINF-05"]
labels: ["devops", "phase2"]
prd_references: ["§2"]
modules_or_screens: ["Infra (production deployment)"]
testing_tiers: ["integration (Testcontainers)"]
---

# PINF-07: Build a blue/green or canary deployment pipeline

## Summary (business)
This story introduces a safer way to roll out new versions of the platform, gradually shifting customer traffic to the new version (or switching over instantly with an instant fallback option) instead of an abrupt, all-at-once switch. If something breaks in the new release, the system automatically reverts, protecting the platform's promised uptime during deployments, which is when outages are most likely to happen.

## User Story
**As a** platform reliability owner, **I want** deploy new releases with a blue/green or canary strategy rather than a hard cutover, **so that** the 99.5%+ uptime target (PRD §2) is protected during releases, not just steady-state operation.

## Acceptance Criteria
- Given a new release is deployed, when the pipeline runs, then traffic shifts gradually (canary) or via an instant blue/green swap, with automatic rollback on health-check failure.

## Developer Notes
- **PRD reference(s):** §2 Goals & Success Metrics
- **Module(s)/Screen(s):** Infra (production deployment)
- **Story points:** 5 — CI/CD pipeline enhancement on top of PINF-05's topology.
- **Dependencies:** PINF-05
- **Testing tier(s):** integration (Testcontainers)

## Sub-tasks
- [NEW] Infra: blue/green or canary deployment pipeline with automatic rollback
- [NEW] Infra: README / RULES.md documentation update
- [NEW] Infra: verification step (local run and/or CI check)
