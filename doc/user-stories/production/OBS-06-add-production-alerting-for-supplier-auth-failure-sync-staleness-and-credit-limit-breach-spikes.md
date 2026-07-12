---
id: OBS-06
epic: Production Observability
phase: production
status: not-started
story_points: 5
dependencies: ["HRD-13", "OBS-01"]
labels: ["backend", "observability", "phase2"]
prd_references: ["§10.2", "§10.5"]
modules_or_screens: ["Infra (observability)"]
testing_tiers: ["integration (Testcontainers)"]
---

# OBS-06: Add production alerting for supplier auth failure, sync staleness, and credit-limit breach spikes

## Summary (business)
Certain problems, like a supplier's system rejecting our requests, stale pricing/inventory data, or an unusual burst of customers exceeding their credit limits, need immediate human attention. This story sets up automatic alerts so the on-call team is notified the moment these issues occur and can act right away, rather than discovering them after customers are already affected.

## User Story
**As a** on-call engineer, **I want** be alerted on a supplier auth failure, inventory-sync staleness beyond threshold, or an unusual spike in credit-limit breaches, **so that** PRD §10.2's Super-Admin-alert requirement and §10.5's staleness-alert requirement are wired to real production alerting, not just logged.

## Acceptance Criteria
- Given a supplier auth failure (e.g. Hotelbeds `INVALID_SIGNATURE`) occurs in production, when the alert rule evaluates, then on-call is paged, and that supplier's results are disabled per §10.2.1's error-handling table until resolved.
- Given credit-limit breaches spike unusually across Consultants, when the alert rule evaluates, then on-call is notified of the anomaly for investigation.

## Developer Notes
- **PRD reference(s):** §10.2 Per-Supplier Integration (auth failure alerting); §10.5 Inventory Sync (staleness alerting)
- **Module(s)/Screen(s):** Infra (observability)
- **Story points:** 5 — Alert-rule wiring on top of already-built failure-handling logic (Phase 1's supplier error tables and HRD-13's staleness alerting) plus a new anomaly-spike rule.
- **Dependencies:** HRD-13, OBS-01
- **Testing tier(s):** integration (Testcontainers)

## Sub-tasks
- [NEW] Backend: production alert rules — supplier auth failure, sync staleness, credit-limit-breach spike
- [NEW] Backend: integrationTest — one simulated trigger per alert rule
