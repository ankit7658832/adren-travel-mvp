---
id: OPS-02
epic: DevOps/Infra
phase: mock
status: not-started
story_points: 5
dependencies: ["OPS-01", "FND-11"]
labels: ["devops", "foundation", "security", "phase1"]
prd_references: ["§5.3"]
modules_or_screens: ["Infra (LocalStack)"]
testing_tiers: ["integration (Testcontainers)"]
---

# OPS-02: Provision Adren-owned supplier credentials in LocalStack Secrets Manager

## Summary (business)
This creates sample, non-real login credentials for our travel suppliers (companies like Hotelbeds, STUBA, and TBO that provide hotel and travel inventory) in the practice environment. It lets engineers build and test the connections to these suppliers safely, without risking exposure of real supplier passwords during development.

## User Story
**As a** backend engineer, **I want** have a provisioning script seed LocalStack Secrets Manager with placeholder Hotelbeds/STUBA/TBO/etc. credential entries, **so that** FND-11's Secrets-Manager-by-ARN pattern has real local entries to reference during development and integrationTest runs.

## Acceptance Criteria
- Given the provisioning script is run against a fresh LocalStack instance, when it completes, then one Secrets Manager entry per supplier exists, matching the ARNs FND-11's credential entities expect.

## Developer Notes
- **PRD reference(s):** §5.3 Secrets handling (RULES.md)
- **Module(s)/Screen(s):** Infra (LocalStack)
- **Story points:** 5 — Scripted seed data — mechanical, but exercises the exact mechanism FND-11 depends on.
- **Dependencies:** OPS-01, FND-11
- **Testing tier(s):** integration (Testcontainers)

## Sub-tasks
- [NEW] Infra: LocalStack Secrets Manager seed script (one entry per Adren-owned supplier)
- [NEW] Infra: README / RULES.md documentation update
- [NEW] Infra: verification step (local run and/or CI check)
