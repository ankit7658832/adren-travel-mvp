---
id: ADS-09
epic: Ads/Campaign Management
phase: mock
status: not-started
story_points: 5
dependencies: ["ADS-07"]
labels: ["backend", "frontend", "ads", "phase1"]
prd_references: ["§14.2", "§20.13"]
modules_or_screens: ["ads", "Consultant Dashboard (21.5)"]
testing_tiers: ["unit", "module (@ApplicationModuleTest)", "component test"]
---

# ADS-09: Flow campaign performance data back to the Consultant Dashboard

## Summary (business)
Once a campaign is running, Consultants can see how it's performing - how many people saw it, clicked on it, and how many bookings it led to - right on their dashboard. This helps Consultants understand whether their ad spend is paying off and make better decisions about future campaigns.

## User Story
**As a** Consultant, **I want** see my campaign's impressions, clicks, and attributed bookings on my dashboard, **so that** PRD §14.2 step 7 and §20.13's `performance_snapshot` are surfaced to the Consultant.

## Acceptance Criteria
- Given a Live campaign has performance data, when the Consultant Dashboard's Active Campaigns tab loads, then impressions, clicks, and bookings_attributed are shown per campaign, sourced from `performance_snapshot`.

## Developer Notes
- **PRD reference(s):** §14.2 Flow step 7; §20.13 performance_snapshot
- **Module(s)/Screen(s):** ads, Consultant Dashboard (21.5)
- **Story points:** 5 — Read-side wiring; write-side (mocked Meta insights polling) uses the same MVP-mock boundary as ADS-07.
- **Dependencies:** ADS-07
- **Testing tier(s):** unit, module (@ApplicationModuleTest), component test

## Sub-tasks
- [NEW] Backend: `performance_snapshot` mock-populated on a scheduled interval (MVP)
- [NEW] Backend: `GET /api/v1/campaigns?consultantId=` paginated endpoint
- [NEW] Backend: unit test
- [NEW] Frontend: component test for the Active Campaigns tab (co-developed with HRD-09's Consultant Dashboard)
