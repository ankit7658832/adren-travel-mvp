---
id: PINF-04
epic: Production Infrastructure
phase: production
status: not-started
story_points: 8
dependencies: ["OPS-04"]
labels: ["devops", "phase2"]
prd_references: ["§2"]
modules_or_screens: ["Infra (production database)"]
testing_tiers: ["integration (Testcontainers)"]
---

# PINF-04: Stand up production Postgres with backup/PITR and connection-pool tuning

## Summary (business)
This story sets up the platform's live production database with automated backups and the ability to restore data to a specific point in time if something goes wrong, along with tuning so it can handle real customer traffic smoothly. This protects critical business data, like bookings, from being lost and ensures the platform performs well under real-world load.

## User Story
**As a** platform reliability owner, **I want** have a managed production Postgres (RDS/Aurora) with point-in-time recovery and tuned connection pooling, **so that** the platform's booking-critical data has a real production database, not just Testcontainers-verified local Postgres.

## Acceptance Criteria
- Given a production incident requires point-in-time recovery, when a restore is performed, then data is recoverable to within the defined RPO, and connection pooling is tuned to the platform's real concurrent-load profile.

## Developer Notes
- **PRD reference(s):** §2 Goals & Success Metrics (99.5%+ uptime)
- **Module(s)/Screen(s):** Infra (production database)
- **Story points:** 8 — Managed database provisioning with backup/recovery and pooling tuning — foundational production infra with real operational risk if under-provisioned.
- **Dependencies:** OPS-04
- **Testing tier(s):** integration (Testcontainers)

## Sub-tasks
- [NEW] Infra: production Postgres (RDS/Aurora) with PITR backup policy and tuned connection pooling
- [NEW] Infra: README / RULES.md documentation update
- [NEW] Infra: verification step (local run and/or CI check)
