---
id: OBS-07
epic: Production Observability
phase: production
status: not-started
story_points: 3
dependencies: ["OBS-01", "PINF-05"]
labels: ["backend", "observability", "phase2"]
prd_references: ["§2"]
modules_or_screens: ["Infra (observability)"]
testing_tiers: ["integration (Testcontainers)"]
---

# OBS-07: Build an uptime/SLO dashboard for the 99.5%+ booking-engine target

## Summary (business)
The business has committed to keeping its booking system available and working at least 99.5% of the time. This story builds a dashboard that shows, in real time and historically, whether that reliability promise is actually being met, so leadership always knows the platform's true uptime instead of assuming it.

## User Story
**As a** Super Admin/on-call engineer, **I want** see the booking engine's real-time and historical uptime against the 99.5%+ SLO, **so that** PRD §2's Goals & Success Metrics target is continuously visible, not just assumed met.

## Acceptance Criteria
- Given the uptime dashboard is opened, when it loads, then current and historical uptime against the 99.5%+ SLO is visible, with any SLO-breaching window clearly flagged.

## Developer Notes
- **PRD reference(s):** §2 Goals & Success Metrics (Platform reliability)
- **Module(s)/Screen(s):** Infra (observability)
- **Story points:** 3 — Standard SLO dashboard on top of OBS-01/PINF-05's health-check and tracing infrastructure.
- **Dependencies:** OBS-01, PINF-05
- **Testing tier(s):** integration (Testcontainers)

## Sub-tasks
- [NEW] Backend: uptime/SLO dashboard against the 99.5%+ target
- [NEW] Backend: SLO-breach flagging
