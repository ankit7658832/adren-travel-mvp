---
id: PINF-01
epic: Production Infrastructure
phase: production
status: not-started
story_points: 8
dependencies: ["FND-11", "FND-12", "OPS-02", "OPS-07"]
labels: ["devops", "security", "phase2"]
prd_references: ["§5.3"]
modules_or_screens: ["Infra (production AWS)"]
testing_tiers: ["integration (Testcontainers)"]
---

# PINF-01: Replace LocalStack Secrets Manager with real AWS Secrets Manager and rotation Lambdas

## Summary (business)
This story moves all system passwords and access keys from a test-only setup into a real, secure vault that automatically changes them on a regular schedule. This closes a major security gap so that stolen or outdated credentials can't be used to break into the live platform, without ever causing an outage while the change happens.

## User Story
**As a** platform security owner, **I want** have every credential FND-11/FND-12/OPS-02/OPS-07 sourced from real AWS Secrets Manager with automated rotation, **so that** RULES.md §5.3's rotation-Lambda pattern is live in production, not just LocalStack-emulated.

## Acceptance Criteria
- Given a production credential's rotation schedule triggers, when the rotation Lambda runs, then the credential rotates with no service disruption, and every consumer resolves the new value via the same ARN.

## Developer Notes
- **PRD reference(s):** §5.3 Secrets handling (RULES.md)
- **Module(s)/Screen(s):** Infra (production AWS)
- **Story points:** 8 — Real AWS Secrets Manager + rotation Lambda wiring, replacing the entire LocalStack-emulated foundation from OPS-01/02/07.
- **Dependencies:** FND-11, FND-12, OPS-02, OPS-07
- **Testing tier(s):** integration (Testcontainers)

## Sub-tasks
- [NEW] Infra: real AWS Secrets Manager + rotation Lambda per credential family (supplier, BYOS, Meta, Groq)
- [NEW] Infra: README / RULES.md documentation update
- [NEW] Infra: verification step (local run and/or CI check)
