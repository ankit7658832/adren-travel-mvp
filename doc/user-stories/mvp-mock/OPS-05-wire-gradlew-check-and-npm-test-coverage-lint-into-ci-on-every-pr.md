---
id: OPS-05
epic: DevOps/Infra
phase: mock
status: not-started
story_points: 5
dependencies: ["FND-19", "OPS-04"]
labels: ["devops", "foundation", "phase1"]
prd_references: ["§8"]
modules_or_screens: ["Infra (CI)"]
testing_tiers: ["integration (Testcontainers)"]
---

# OPS-05: Wire ./gradlew check and npm test/coverage/lint into CI on every PR

## Summary (business)
This ensures that every proposed code change is automatically checked for quality problems and broken rules before it can be merged into the product. It catches bugs, poor test coverage, and rule violations early, reducing the chance that a mistake reaches customers and saving the team from costly fixes later.

## User Story
**As a** engineering team, **I want** have every PR automatically run the full backend (`./gradlew check`) and frontend (`npm run test:coverage`, `npm run lint`) gates, **so that** module-boundary violations (`ModularityTests`), coverage regressions, and lint failures are caught before merge, not discovered later.

## Acceptance Criteria
- Given a PR is opened, when CI runs, then `./gradlew check`, `npm run test:coverage`, and `npm run lint` all execute and the PR is blocked from merge if any fails.

## Developer Notes
- **PRD reference(s):** §8 PR / Code Review Checklist (RULES.md)
- **Module(s)/Screen(s):** Infra (CI)
- **Story points:** 5 — Standard CI pipeline wiring; the specific gate list (check vs. test, coverage thresholds, lint) matters more than the CI platform mechanics.
- **Dependencies:** FND-19, OPS-04
- **Testing tier(s):** integration (Testcontainers)

## Sub-tasks
- [NEW] Infra: CI pipeline configuration running `./gradlew check` + `npm run test:coverage` + `npm run lint` on every PR
- [NEW] Infra: README / RULES.md documentation update
- [NEW] Infra: verification step (local run and/or CI check)
