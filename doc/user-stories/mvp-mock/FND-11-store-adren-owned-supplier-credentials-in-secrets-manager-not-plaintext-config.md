---
id: FND-11
epic: Foundation
phase: mock
status: not-started
story_points: 5
dependencies: ["FND-10"]
labels: ["backend", "foundation", "security", "phase1"]
prd_references: ["§5.3"]
modules_or_screens: ["supplier"]
testing_tiers: ["unit", "integration (Testcontainers + LocalStack)"]
---

# FND-11: Store Adren-owned supplier credentials in Secrets Manager, not plaintext config

## Summary (business)
This story ensures that the platform's own supplier connection credentials are stored in a dedicated, secure vault rather than in plain text files or settings that could be accidentally exposed. This significantly reduces the risk of a credential leak that could let outsiders access supplier accounts.

## User Story
**As a** platform security owner, **I want** have Hotelbeds/STUBA/TBO/etc. credentials live in AWS Secrets Manager (LocalStack in dev) referenced by ARN, **so that** no real integration credential is ever a plaintext config value, committed file, or environment variable outside local Docker Compose, per RULES.md §5.3.

## Acceptance Criteria
- Given a supplier credential is saved via FND-10's screen, when the write completes, then the secret is stored in Secrets Manager and only its ARN is persisted in Postgres.
- Given application config is inspected in any non-local profile, when no supplier credential appears, then only ARNs/references are present.

## Developer Notes
- **PRD reference(s):** §5.3 Secrets handling (RULES.md)
- **Module(s)/Screen(s):** supplier
- **Story points:** 5 — Wires `aws-secretsmanager` (already a build dependency) into the credential save path.
- **Dependencies:** FND-10
- **Testing tier(s):** unit, integration (Testcontainers + LocalStack)

## Sub-tasks
- [NEW] Backend: Secrets Manager write on credential save (LocalStack-backed in dev/test)
- [EXTEND] Backend: `SupplierCredential` entity stores ARN only
- [NEW] Backend: unit test (ARN persisted, no plaintext)
- [NEW] Backend: integrationTest against LocalStack Secrets Manager
