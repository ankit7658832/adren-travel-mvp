---
id: OPS-08
epic: DevOps/Infra
phase: mock
status: not-started
story_points: 2
dependencies: []
labels: ["devops", "foundation", "phase1"]
prd_references: ["backend-spring-modulith skill (module doc generation)"]
modules_or_screens: ["Infra (release process)"]
testing_tiers: ["integration (Testcontainers)"]
---

# OPS-08: Wire module-documentation generation into the release checklist

## Summary (business)
This adds a step to the release process that automatically keeps a diagram of how the system's different parts fit together up to date whenever the product is released. This prevents the team's technical documentation from becoming outdated or misleading, which helps new engineers and reviewers understand the system accurately.

## User Story
**As a** engineering team, **I want** have `ModularityTests.writeModuleDocumentation()`'s PlantUML output copied into `doc/architecture/` as a standard release step, **so that** the module map in documentation can't silently drift from the actual code structure, per the backend-spring-modulith skill.

## Acceptance Criteria
- Given a release checklist is run, when the module-documentation step executes, then `build/spring-modulith-docs/*.puml` is regenerated and copied into `doc/architecture/`, and the diff is reviewed as part of the release PR.

## Developer Notes
- **PRD reference(s):** backend-spring-modulith skill (module doc generation)
- **Module(s)/Screen(s):** Infra (release process)
- **Story points:** 2 — Process/checklist wiring, not new code — codifies an already-documented but not-yet-enforced step.
- **Dependencies:** None
- **Testing tier(s):** integration (Testcontainers)

## Sub-tasks
- [NEW] Infra: Release checklist step: regenerate and copy PlantUML module docs into `doc/architecture/`
- [NEW] Infra: README / RULES.md documentation update
- [NEW] Infra: verification step (local run and/or CI check)
