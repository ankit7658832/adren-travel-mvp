---
id: TST-01
epic: Test Infrastructure
phase: mock
status: not-started
story_points: 5
dependencies: ["OPS-01"]
labels: ["backend", "testing", "foundation", "phase1"]
prd_references: ["testing-strategy skill (End-to-end tier)"]
modules_or_screens: ["Infra (test)"]
testing_tiers: ["integration (Testcontainers)"]
---

# TST-01: Extend the Testcontainers base infrastructure for new modules

## Summary (business)
As new parts of the product (like AI features, payments, white-label branding, ads, and compliance) move from placeholder to fully working, this work ensures each one is checked with the same thorough, realistic testing setup already used for bookings. This reduces the chance that a newly-built area ships with undetected bugs simply because it wasn't tested as rigorously as older features.

## User Story
**As a** backend engineer, **I want** have `TestInfrastructure` provide Postgres + LocalStack containers usable by every module's `integrationTest`, not just `booking`'s, **so that** as `ai`/`payments`/`whitelabel`/`ads`/`compliance` move from stub to real modules, each gets the same Testcontainers foundation `BookingEndToEndIT` already established.

## Acceptance Criteria
- Given a new module's `integrationTest` extends the shared base, when it runs, then it gets a real Postgres + LocalStack (S3/Secrets Manager/KMS as needed) without redefining container setup.

## Developer Notes
- **PRD reference(s):** testing-strategy skill (End-to-end tier)
- **Module(s)/Screen(s):** Infra (test)
- **Story points:** 5 — Extends the existing reference `TestInfrastructure.java` to cover the LocalStack services OPS-01 adds.
- **Dependencies:** OPS-01
- **Testing tier(s):** integration (Testcontainers)

## Sub-tasks
- [NEW] Test infra: `TestInfrastructure` extended with S3/Secrets Manager/KMS LocalStack containers
- [NEW] Test infra: sample test exercising the new harness
- [NEW] Test infra: CI wiring / gradle-or-npm script update
