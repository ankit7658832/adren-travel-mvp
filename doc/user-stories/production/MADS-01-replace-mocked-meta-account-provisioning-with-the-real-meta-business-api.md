---
id: MADS-01
epic: Meta Ads API Real Integration
phase: production
status: not-started
story_points: 8
dependencies: ["ADS-01"]
labels: ["backend", "ads", "phase2"]
prd_references: ["§14.1"]
modules_or_screens: ["ads"]
testing_tiers: ["unit", "integration (Testcontainers)"]
---

# MADS-01: Replace mocked Meta account provisioning with the real Meta Business API

## Summary (business)
This story replaces our test/practice version of setting up advertising accounts with the real thing: when Adren sets up a Facebook/Instagram (Meta) advertising account for a travel consultant, it will actually create that account on Meta's systems instead of just recording it in our own records. This is the essential first step before any real money can be spent on ads.

## User Story
**As a** Super Admin, **I want** provision a real Meta ad account and Business Manager for a Consultant via the Meta Business API, **so that** ADS-01's MVP-mocked provisioning is replaced with a real integration before any real ad spend can occur.

## Acceptance Criteria
- Given Super Admin provisions a Meta ad account for a Consultant, when the real API call is made, then a genuine Meta Business Manager and ad account are created under Adren's umbrella structure, replacing ADS-01's mocked bookkeeping-only entity.

## Developer Notes
- **PRD reference(s):** §14.1 Ads/Campaign Overview
- **Module(s)/Screen(s):** ads
- **Story points:** 8 — First real external Meta API integration — carries real account-liability risk per PRD §7's named risk.
- **Dependencies:** ADS-01
- **Testing tier(s):** unit, integration (Testcontainers)

## Sub-tasks
- [EXTEND] Backend: `AdAccount` provisioning calls the real Meta Business API
- [NEW] Backend: Meta API credential handling via the same Secrets Manager pattern as FND-11
- [NEW] Backend: unit test — request/response mapping
- [NEW] Backend: integrationTest against Meta's sandbox/test environment
