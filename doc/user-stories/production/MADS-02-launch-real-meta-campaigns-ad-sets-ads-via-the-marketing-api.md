---
id: MADS-02
epic: Meta Ads API Real Integration
phase: production
status: not-started
story_points: 8
dependencies: ["MADS-01", "ADS-07"]
labels: ["backend", "ads", "phase2"]
prd_references: ["§14.2"]
modules_or_screens: ["ads"]
testing_tiers: ["unit", "integration (Testcontainers)"]
---

# MADS-02: Launch real Meta Campaigns/Ad Sets/Ads via the Marketing API

## Summary (business)
Once a consultant's ad campaign is approved, it will now actually go live on Facebook/Instagram instead of just being marked as launched in our internal records. This ensures customers genuinely see the ads that consultants believe they've published.

## User Story
**As a** Consultant, **I want** have my approved campaign actually launch on Meta, not just record a mocked `meta_campaign_ref`, **so that** ADS-07's MVP-mocked launch call is replaced with the real Meta Marketing API.

## Acceptance Criteria
- Given a campaign passes policy review, when launch is triggered, then a real Meta Campaign/Ad Set/Ad hierarchy is created via the Marketing API and the real `meta_campaign_ref` is stored.

## Developer Notes
- **PRD reference(s):** §14.2 Flow step 6
- **Module(s)/Screen(s):** ads
- **Story points:** 8 — Real Marketing API object-hierarchy creation (Campaign→AdSet→Ad) is materially more complex than the MVP's single mocked call.
- **Dependencies:** MADS-01, ADS-07
- **Testing tier(s):** unit, integration (Testcontainers)

## Sub-tasks
- [EXTEND] Backend: real Marketing API Campaign/AdSet/Ad creation replacing ADS-07's mock
- [NEW] Backend: unit test
- [NEW] Backend: integrationTest against Meta's sandbox
