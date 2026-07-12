---
id: LLM-05
epic: LLM Production Readiness
phase: production
status: not-started
story_points: 5
dependencies: ["AI-13", "OBS-01"]
labels: ["backend", "ai", "phase2"]
prd_references: ["§24.3"]
modules_or_screens: ["ai"]
testing_tiers: ["integration (Testcontainers)"]
---

# LLM-05: Monitor and alert on AI response latency SLOs in production

## Summary (business)
We will set up automatic alerts that notify our technical team the moment AI-generated suggestions start taking too long to come back. This keeps us in front of performance problems so customers still get their itinerary within our promised fast turnaround time.

## User Story
**As a** on-call engineer, **I want** be alerted when AI suggestion latency breaches the per-segment SLO that protects the 10-minute itinerary target, **so that** PRD §24.3's NFR is continuously monitored in production, not just tested once pre-launch.

## Acceptance Criteria
- Given AI suggestion latency exceeds the defined per-segment SLO in production, when the monitoring threshold is breached, then an alert fires to on-call before the 10-minute end-to-end itinerary target is meaningfully at risk.

## Developer Notes
- **PRD reference(s):** §24.3 NFR AI Governance
- **Module(s)/Screen(s):** ai
- **Story points:** 5 — Dashboard/alert wiring on top of AI-13's bounded-timeout mechanism and OBS-01's tracing.
- **Dependencies:** AI-13, OBS-01
- **Testing tier(s):** integration (Testcontainers)

## Sub-tasks
- [NEW] Backend: AI latency SLO dashboard + alert rule
- [NEW] Backend: integrationTest — simulated SLO breach triggers the alert path
