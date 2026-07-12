---
id: OPS-09
epic: DevOps/Infra
phase: mock
status: not-started
story_points: 3
dependencies: ["OPS-02", "OPS-07"]
labels: ["devops", "foundation", "phase1"]
prd_references: ["§5.3"]
modules_or_screens: ["Infra (config)"]
testing_tiers: ["integration (Testcontainers)"]
---

# OPS-09: Define per-environment application.yml profiles

## Summary (business)
This creates clearly separated settings for the local development, testing, and staging environments, so that shortcuts allowed only for developers' personal machines (like using sample passwords) can never accidentally be used in a shared or production-like environment. This reduces the risk of real credentials being exposed or misconfigured as the product moves toward launch.

## User Story
**As a** backend engineer, **I want** have distinct `local`/`test`/`staging` Spring profiles with the correct secret-sourcing and service-endpoint boundaries per profile, **so that** the local-only plaintext credential exception in RULES.md §5.3 stays scoped to `local` and never leaks into a shared profile.

## Acceptance Criteria
- Given the application starts under the `local` profile, when config resolves, then plaintext local Docker Compose credentials are used.
- Given the application starts under any non-local profile, when config resolves, then every credential resolves via Secrets Manager by ARN — no plaintext fallback exists in that profile's config.

## Developer Notes
- **PRD reference(s):** §5.3 Secrets handling (RULES.md)
- **Module(s)/Screen(s):** Infra (config)
- **Story points:** 3 — Config-file structuring; the guarantee is the absence of a plaintext fallback outside `local`, which needs its own check.
- **Dependencies:** OPS-02, OPS-07
- **Testing tier(s):** integration (Testcontainers)

## Sub-tasks
- [NEW] Infra: `application-local.yml` / `application-test.yml` / `application-staging.yml` profile split
- [NEW] Infra: README / RULES.md documentation update
- [NEW] Infra: verification step (local run and/or CI check)
