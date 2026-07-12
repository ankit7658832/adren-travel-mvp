---
id: TST-02
epic: Test Infrastructure
phase: mock
status: not-started
story_points: 3
dependencies: ["TST-01"]
labels: ["backend", "testing", "foundation", "phase1"]
prd_references: ["backend-spring-modulith skill (Verifying boundaries)"]
modules_or_screens: ["Infra (test)"]
testing_tiers: ["unit"]
---

# TST-02: Extend ModularityTests coverage as stub modules become real

## Summary (business)
As currently-placeholder areas of the product (AI, payments, white-labeling, ads, compliance) start containing real functionality, this ensures our automated 'keep the architecture clean' checks automatically start covering them too, rather than silently skipping them. Without this, a growing part of the system could quietly lose quality oversight, increasing the risk of costly design problems later.

## User Story
**As a** backend engineer, **I want** have `ModularityTests.moduleBoundariesAreRespected()` continue passing as `ai`/`payments`/`whitelabel`/`ads`/`compliance` gain real code, **so that** the module-boundary enforcement mechanism doesn't silently stop covering a module just because it moved past package-info-only stub status.

## Acceptance Criteria
- Given any module gains its first real `internal/` class, when `./gradlew check` runs, then `ApplicationModules.verify()` includes that module in its boundary check with no manual test-list update required.

## Developer Notes
- **PRD reference(s):** backend-spring-modulith skill (Verifying boundaries)
- **Module(s)/Screen(s):** Infra (test)
- **Story points:** 3 — `ApplicationModules.of(...)` already scans the full application context — this story is verifying/documenting that no manual step is needed, plus a regression test.
- **Dependencies:** TST-01
- **Testing tier(s):** unit

## Sub-tasks
- [EXTEND] Backend: regression test confirming `ApplicationModules.verify()` auto-includes newly-real modules
- [NEW] Backend: CI assertion — a deliberately-introduced cross-module `.internal` import fails the build
