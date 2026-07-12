---
id: ADS-07
epic: Ads/Campaign Management
phase: mock
status: not-started
story_points: 5
dependencies: ["ADS-06", "ADS-01"]
labels: ["backend", "ads", "phase1"]
prd_references: ["§14.2", "§20.13"]
modules_or_screens: ["ads"]
testing_tiers: ["unit", "module (@ApplicationModuleTest)"]
---

# ADS-07: Launch an approved campaign under the Adren-managed Meta account

## Summary (business)
Once a campaign clears every review step, it is switched on and starts running as a real advertisement (using a simulated connection to the ad platform for now, with the full live connection planned for a later phase). This is the moment a Consultant's campaign actually starts reaching potential customers.

## User Story
**As a** Consultant, **I want** have my approved campaign go live and receive a `meta_campaign_ref`, **so that** PRD §14.2 step 6 is implemented (mocked Meta launch call in MVP; real integration is Phase 2's MADS epic).

## Acceptance Criteria
- Given a campaign passes policy review, when launch is triggered, then status transitions to Live and a `meta_campaign_ref` is stored, using a mocked Meta launch call in MVP scope.

## Developer Notes
- **PRD reference(s):** §14.2 Flow step 6; §20.13 meta_campaign_ref
- **Module(s)/Screen(s):** ads
- **Story points:** 5 — Third state-machine transition; the mock/real boundary is deliberate MVP scoping (see Phase 2 MADS-02 for the live equivalent).
- **Dependencies:** ADS-06, ADS-01
- **Testing tier(s):** unit, module (@ApplicationModuleTest)

## Sub-tasks
- [EXTEND] Backend: `launchCampaign` business logic / state-transition method
- [EXTEND] Backend: domain event publication (`@Transactional`, same method scope)
- [EXTEND] Backend: REST endpoint `POST /api/v1/campaigns/{id}/launch`
- [NEW] Backend: unit test
- [NEW] Backend: module/integration test
