---
id: PINF-11
epic: Production Infrastructure
phase: production
status: not-started
story_points: 5
dependencies: ["PINF-04"]
labels: ["devops", "phase2"]
prd_references: ["§2"]
modules_or_screens: ["Infra (production)"]
testing_tiers: ["integration (Testcontainers)"]
---

# PINF-11: Run a disaster-recovery/backup-restore drill for booking-critical data

## Summary (business)
This story is a real, hands-on test to confirm that critical business data, such as bookings, customer wallet balances, and traveler profiles, can actually be recovered from backup within an acceptable time and with minimal data loss if a disaster occurs. It proves the platform's disaster-recovery plan actually works in practice, rather than assuming it would work because it was set up correctly.

## User Story
**As a** platform reliability owner, **I want** prove that booking-critical data (bookings, wallet ledger, traveler profiles) can actually be restored from backup within the defined RPO/RTO, **so that** PINF-04's PITR capability is verified by a real drill, not just assumed to work because it's configured.

## Acceptance Criteria
- Given a DR drill is run against a production-equivalent environment, when a restore is performed, then booking-critical data is recovered within the defined RPO/RTO, and the drill's results are documented.

## Developer Notes
- **PRD reference(s):** §2 Goals & Success Metrics (99.5%+ uptime)
- **Module(s)/Screen(s):** Infra (production)
- **Story points:** 5 — Operational verification exercise against PINF-04's backup infrastructure.
- **Dependencies:** PINF-04
- **Testing tier(s):** integration (Testcontainers)

## Sub-tasks
- [NEW] Infra: DR/backup-restore drill against a production-equivalent environment, results documented
- [NEW] Infra: README / RULES.md documentation update
- [NEW] Infra: verification step (local run and/or CI check)
