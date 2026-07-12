---
id: MADS-03
epic: Meta Ads API Real Integration
phase: production
status: not-started
story_points: 5
dependencies: ["MADS-02"]
labels: ["backend", "ads", "phase2"]
prd_references: ["§14.2", "§14.4"]
modules_or_screens: ["ads"]
testing_tiers: ["unit", "integration (Testcontainers)"]
---

# MADS-03: Upload real creative (image/copy) via the Meta Marketing API

## Summary (business)
The pictures and text a consultant has approved for their ad will now be sent to Facebook/Instagram so they can actually be used in the live ad, rather than just being saved in our own system. Without this, an approved ad would have no real content to show.

## User Story
**As a** Consultant, **I want** have my approved creative variants actually uploaded to Meta, not just stored locally, **so that** ADS-04's locally-persisted `creative_variants[]` are pushed to Meta as real ad creative assets.

## Acceptance Criteria
- Given a creative variant is Consultant-approved (ADS-05), when the campaign launches (MADS-02), then the approved image/copy is uploaded to Meta as a real ad-creative asset referenced by the launched Ad.

## Developer Notes
- **PRD reference(s):** §14.2 Flow step 3; §14.4 AI Creative Generation
- **Module(s)/Screen(s):** ads
- **Story points:** 5 — Asset-upload API call layered onto MADS-02's launch flow.
- **Dependencies:** MADS-02
- **Testing tier(s):** unit, integration (Testcontainers)

## Sub-tasks
- [NEW] Backend: Meta creative-asset upload call
- [NEW] Backend: unit test
- [NEW] Backend: integrationTest against Meta's sandbox
