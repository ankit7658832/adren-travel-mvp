---
id: FND-12
epic: Foundation
phase: mock
status: not-started
story_points: 8
dependencies: ["FND-03", "FND-11"]
labels: ["backend", "foundation", "security", "phase1"]
prd_references: ["§5.3", "§10.4"]
modules_or_screens: ["supplier"]
testing_tiers: ["unit", "integration (Testcontainers + LocalStack KMS)"]
---

# FND-12: Store BYOS credentials as row-level, per-Consultant encrypted secrets

## Summary (business)
This story ensures that when a consultant brings and stores their own supplier credentials (rather than using Adren's), those credentials are encrypted individually per consultant and can never be read by another consultant. This protects each consultant's private business relationships with their own suppliers.

## User Story
**As a** Consultant, **I want** have my BYOS supplier credentials encrypted at the row level and reachable only through my own tenant scope, **so that** another Consultant's BYOS credential read is never possible through the same lookup key, per RULES.md §5.3 and PRD §10.4.

## Acceptance Criteria
- Given a Consultant saves their own Hotelbeds BYOS credentials, when the write completes, then the ciphertext is stored per-row with a KMS-wrapped data key, not in the shared Secrets Manager entry used for Adren's own credentials.
- Given Consultant B's service call attempts to read Consultant A's BYOS credential row, when the tenant-isolation check runs, then access is denied.

## Developer Notes
- **PRD reference(s):** §5.3 Secrets handling (RULES.md); §10.4 BYOS
- **Module(s)/Screen(s):** supplier
- **Story points:** 8 — KMS envelope encryption + per-row secret pattern is materially different from FND-11's Secrets-Manager-by-ARN pattern — new mechanism, high blast-radius if wrong.
- **Dependencies:** FND-03, FND-11
- **Testing tier(s):** unit, integration (Testcontainers + LocalStack KMS)

## Sub-tasks
- [NEW] Backend: `ByosCredential` entity — ciphertext + wrapped data key columns
- [NEW] Backend: KMS envelope encryption service (LocalStack KMS in dev/test)
- [NEW] Backend: tenant-scoped read path (reuses FND-03's isolation check)
- [NEW] Backend: unit test (encryption round-trip)
- [NEW] Backend: integrationTest — cross-tenant read attempt denied
