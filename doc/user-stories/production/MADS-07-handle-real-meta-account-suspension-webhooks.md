---
id: MADS-07
epic: Meta Ads API Real Integration
phase: production
status: not-started
story_points: 5
dependencies: ["MADS-01", "ADS-13"]
labels: ["backend", "ads", "phase2"]
prd_references: ["§23.5", "§25"]
modules_or_screens: ["ads"]
testing_tiers: ["unit", "module (@ApplicationModuleTest)"]
---

# MADS-07: Handle real Meta account-suspension webhooks

## Summary (business)
If Facebook/Instagram suspends a consultant's advertising account (for example, due to a policy issue), Adren will now automatically detect that in real time and immediately flag every affected campaign as needing the consultant's attention. This ensures consultants aren't left running ads on an account that has actually been shut down.

## User Story
**As a** Consultant, **I want** see the 'suspended — action required' status (ADS-13) triggered by a real Meta suspension event, not a mocked signal, **so that** PRD §23.5 Edge Case #12 and T17 hold at production scale against Meta's real webhook delivery.

## Acceptance Criteria
- Given Meta suspends a real ad account mid-campaign, when the suspension webhook is received, then every active campaign under that Consultant transitions to ADS-13's 'suspended — action required' status via the real webhook, replacing the MVP's mocked suspension-signal handler.

## Developer Notes
- **PRD reference(s):** §23.5 Edge Case #12; §25 T17
- **Module(s)/Screen(s):** ads
- **Story points:** 5 — Replaces ADS-13's mocked signal handler with a real, authenticated Meta webhook receiver.
- **Dependencies:** MADS-01, ADS-13
- **Testing tier(s):** unit, module (@ApplicationModuleTest)

## Sub-tasks
- [EXTEND] Backend: real Meta webhook receiver (authenticated, signature-verified) replacing the mocked suspension handler
- [NEW] Backend: unit test
- [NEW] Backend: module test
