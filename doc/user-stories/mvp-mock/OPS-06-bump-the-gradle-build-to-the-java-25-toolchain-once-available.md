---
id: OPS-06
epic: DevOps/Infra
phase: mock
status: not-started
story_points: 2
dependencies: []
labels: ["devops", "foundation", "phase1"]
prd_references: ["backend/README.md toolchain note"]
modules_or_screens: ["Infra (Gradle)"]
testing_tiers: ["integration (Testcontainers)"]
---

# OPS-06: Bump the Gradle build to the Java 25 toolchain once available

## Summary (business)
This is a planned future upgrade to move the backend software onto a newer, supported version of its core programming platform once that version becomes available. Keeping the platform current helps ensure the product stays secure, supported, and free of compatibility issues down the road.

## User Story
**As a** backend engineer, **I want** build against the Java 25 toolchain the project targets, once a Java 25 JDK is available in the build environment, **so that** the scaffold, currently authored against a Java 21 JDK per backend/README's own note, stops carrying a known version gap.

## Acceptance Criteria
- Given a Java 25 JDK is available in the build environment, when `./gradlew build` is run, then it builds against the Java 25 toolchain with no compatibility warnings.

## Developer Notes
- **PRD reference(s):** backend/README.md toolchain note
- **Module(s)/Screen(s):** Infra (Gradle)
- **Story points:** 2 — Toolchain version bump — small, but explicitly flagged as a known gap in the existing reference implementation.
- **Dependencies:** None
- **Testing tier(s):** integration (Testcontainers)

## Sub-tasks
- [NEW] Infra: `build.gradle.kts` toolchain version bump to Java 25 (once available)
- [NEW] Infra: README / RULES.md documentation update
- [NEW] Infra: verification step (local run and/or CI check)
