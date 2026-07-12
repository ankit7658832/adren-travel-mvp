---
id: OPS-01
epic: DevOps/Infra
phase: mock
status: not-started
story_points: 5
dependencies: []
labels: ["devops", "foundation", "phase1"]
prd_references: ["§5"]
modules_or_screens: ["Infra (docker-compose)"]
testing_tiers: ["integration (Testcontainers)"]
---

# OPS-01: Extend docker-compose with LocalStack S3/SQS/SNS/Secrets Manager/KMS services

## Summary (business)
This sets up a realistic practice environment where engineers can build and test features that depend on cloud services (like file storage and secure credential storage) without touching real, costly cloud infrastructure. It lets the team safely develop and verify features that store secrets and files before those features go live, reducing the risk of mistakes reaching customers.

## User Story
**As a** backend engineer, **I want** have every AWS-shaped service the MVP needs available locally via LocalStack, not just the current Postgres+base-LocalStack baseline, **so that** FND-11/FND-12/BOK-15's Secrets Manager, KMS, and S3 dependencies all have a local dev target before any of those stories can be verified.

## Acceptance Criteria
- Given `docker compose up -d` is run, when the stack starts, then Postgres, LocalStack S3, SQS, SNS, Secrets Manager, and KMS are all available and reachable from `bootRun`.

## Developer Notes
- **PRD reference(s):** §5 System Architecture Overview
- **Module(s)/Screen(s):** Infra (docker-compose)
- **Story points:** 5 — Extends the existing reference `docker-compose.yml` — mechanical service addition, but a blocking prerequisite for several FND/BOK/FIN stories.
- **Dependencies:** None
- **Testing tier(s):** integration (Testcontainers)

## Sub-tasks
- [NEW] Infra: `docker-compose.yml` LocalStack service list extended to S3/SQS/SNS/Secrets Manager/KMS
- [NEW] Infra: README / RULES.md documentation update
- [NEW] Infra: verification step (local run and/or CI check)
