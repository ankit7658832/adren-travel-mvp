---
id: PINF-08
epic: Production Infrastructure
phase: production
status: not-started
story_points: 3
dependencies: ["OPS-04", "PINF-04"]
labels: ["devops", "phase2"]
prd_references: ["§4.2"]
modules_or_screens: ["Infra (production database)"]
testing_tiers: ["integration (Testcontainers)"]
---

# PINF-08: Write a production Flyway migration rollout/rollback runbook

## Summary (business)
This story creates a documented, step-by-step procedure for engineers to follow when updating or, if necessary, undoing changes to the production database structure. Having a clear, tested process reduces the risk of mistakes or extended downtime when the database needs to change after the platform is already live and serving customers.

## User Story
**As a** backend engineer, **I want** have a documented runbook for rolling out and, if needed, rolling back a production migration, **so that** OPS-04's migration discipline extends to a real production operational procedure, given migrations are additive-only and can't simply be reverted in place.

## Acceptance Criteria
- Given a production migration needs to be rolled back, when the runbook is followed, then the rollback is executed as a new additive migration reversing the change, never an edit to the already-merged migration, per RULES.md §4.2.

## Developer Notes
- **PRD reference(s):** §4.2 Migration discipline (RULES.md)
- **Module(s)/Screen(s):** Infra (production database)
- **Story points:** 3 — Documentation deliverable formalizing OPS-04's discipline for the production operational context.
- **Dependencies:** OPS-04, PINF-04
- **Testing tier(s):** integration (Testcontainers)

## Sub-tasks
- [NEW] Infra: production Flyway rollout/rollback runbook
- [NEW] Infra: README / RULES.md documentation update
- [NEW] Infra: verification step (local run and/or CI check)
