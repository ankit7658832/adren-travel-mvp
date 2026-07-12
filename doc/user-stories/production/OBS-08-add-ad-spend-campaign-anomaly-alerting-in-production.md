---
id: OBS-08
epic: Production Observability
phase: production
status: not-started
story_points: 5
dependencies: ["MADS-05", "OBS-01"]
labels: ["backend", "observability", "ads", "phase2"]
prd_references: ["§14.3", "§24.6"]
modules_or_screens: ["ads (observability)"]
testing_tiers: ["integration (Testcontainers)"]
---

# OBS-08: Add ad-spend/campaign anomaly alerting in production

## Summary (business)
Advertising budgets can be wasted quickly if a marketing campaign's spending suddenly behaves unexpectedly. This story adds automatic alerts that flag unusual spending patterns as they emerge, so a manager can step in and control costs early, instead of only finding out once the campaign has already hit its budget cap.

## User Story
**As a** Super Admin, **I want** be alerted if a campaign's real Meta spend pattern looks anomalous, **so that** PRD §14.3's guardrail and §24.6's near-real-time NFR are backed by proactive alerting, not just MADS-05's reconciliation-on-read enforcement.

## Acceptance Criteria
- Given a Live campaign's real Meta spend rate deviates sharply from its historical/expected pace, when the anomaly rule evaluates, then Super Admin is alerted proactively, ahead of MADS-05's spend-cap reconciliation catching it only at the cap boundary.

## Developer Notes
- **PRD reference(s):** §14.3 Controls & Guardrails; §24.6 NFR Ads/Campaign
- **Module(s)/Screen(s):** ads (observability)
- **Story points:** 5 — Proactive anomaly-detection alerting layered on top of MADS-04/MADS-05's already-built spend-tracking data.
- **Dependencies:** MADS-05, OBS-01
- **Testing tier(s):** integration (Testcontainers)

## Sub-tasks
- [NEW] Backend: ad-spend anomaly-detection alert rule
- [NEW] Backend: integrationTest — simulated anomalous spend pattern triggers the alert
