---
id: PINF-10
epic: Production Infrastructure
phase: production
status: not-started
story_points: 5
dependencies: ["PINF-01", "MADS-01"]
labels: ["devops", "security", "phase2"]
prd_references: ["§5.3"]
modules_or_screens: ["Infra (production)"]
testing_tiers: ["integration (Testcontainers)"]
---

# PINF-10: Establish production secrets rotation for Adren-owned supplier and Meta credentials

## Summary (business)
This story ensures that Adren's own login credentials for its travel suppliers and Meta (Facebook/Instagram) advertising accounts are automatically refreshed on a set schedule in the live environment. Because these are especially high-value and sensitive credentials, keeping them current and secure prevents service interruptions or security incidents that could affect bookings or ad campaigns.

## User Story
**As a** platform security owner, **I want** have Adren's own supplier and Meta credentials rotate on a defined production schedule, **so that** PINF-01's rotation mechanism is specifically applied and scheduled for the highest-value credential families (per RULES.md §5.3's explicit Meta-credential risk note).

## Acceptance Criteria
- Given a supplier or Meta credential's rotation schedule triggers in production, when rotation completes, then every live supplier/Meta integration continues functioning without a manual credential-swap step.

## Developer Notes
- **PRD reference(s):** §5.3 Secrets handling (RULES.md)
- **Module(s)/Screen(s):** Infra (production)
- **Story points:** 5 — Applies PINF-01's rotation mechanism specifically to supplier + Meta credentials with a defined schedule.
- **Dependencies:** PINF-01, MADS-01
- **Testing tier(s):** integration (Testcontainers)

## Sub-tasks
- [NEW] Infra: production rotation schedule for supplier and Meta credentials
- [NEW] Infra: README / RULES.md documentation update
- [NEW] Infra: verification step (local run and/or CI check)
