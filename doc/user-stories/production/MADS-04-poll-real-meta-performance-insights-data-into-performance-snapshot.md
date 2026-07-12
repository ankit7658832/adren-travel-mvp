---
id: MADS-04
epic: Meta Ads API Real Integration
phase: production
status: not-started
story_points: 5
dependencies: ["MADS-02", "ADS-09"]
labels: ["backend", "ads", "phase2"]
prd_references: ["§14.2", "§20.13"]
modules_or_screens: ["ads"]
testing_tiers: ["unit", "integration (Testcontainers)"]
---

# MADS-04: Poll real Meta performance/insights data into performance_snapshot

## Summary (business)
Consultants will see real, up-to-date numbers on how their ads are performing (how many people saw or clicked them, and how many resulted in bookings) pulled directly from Facebook/Instagram, instead of practice numbers used during early testing. This gives consultants trustworthy data to judge whether their advertising is working.

## User Story
**As a** Consultant, **I want** see real impressions, clicks, and attributed bookings from Meta, not ADS-09's mocked scheduled data, **so that** PRD §14.2 step 7's performance flow-back is backed by the real Meta Insights API.

## Acceptance Criteria
- Given a Live campaign has accrued real Meta spend/engagement, when the polling job runs, then `performance_snapshot` reflects real Meta Insights API data, replacing ADS-09's mocked interval-populated figures.

## Developer Notes
- **PRD reference(s):** §14.2 Flow step 7; §20.13 performance_snapshot
- **Module(s)/Screen(s):** ads
- **Story points:** 5 — Polling-job integration against a real read-only Meta endpoint — lower risk than the write-path stories in this epic.
- **Dependencies:** MADS-02, ADS-09
- **Testing tier(s):** unit, integration (Testcontainers)

## Sub-tasks
- [EXTEND] Backend: Meta Insights API polling job replacing ADS-09's mock
- [NEW] Backend: unit test
- [NEW] Backend: integrationTest against Meta's sandbox
