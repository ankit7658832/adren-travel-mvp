---
id: ADS-13
epic: Ads/Campaign Management
phase: mock
status: not-started
story_points: 5
dependencies: ["ADS-01", "ADS-07"]
labels: ["backend", "frontend", "ads", "phase1"]
prd_references: ["§23.5", "§25"]
modules_or_screens: ["ads"]
testing_tiers: ["unit", "module (@ApplicationModuleTest)", "component test"]
---

# ADS-13: Surface a clear 'suspended — action required' status on Meta account suspension

## Summary (business)
If the advertising platform (Meta, i.e. Facebook/Instagram's parent company) suspends an account while a campaign is running, Consultants immediately see a clear "suspended - action required" message instead of their campaign just quietly stopping with no explanation. This ensures Consultants are never left confused about why their ads stopped running and know they need to act.

## User Story
**As a** Consultant, **I want** see an explicit, actionable status if Meta suspends my ad account mid-campaign, not a silently-stopped campaign, **so that** PRD §23.5 Edge Case #12 and T17 are satisfied.

## Acceptance Criteria
- Given Meta suspends an ad account mid-campaign (simulated in MVP via a mocked suspension signal), when the suspension is detected, then all active campaigns under that Consultant show 'suspended — action required,' not a silent stop with no explanation (T17).

## Developer Notes
- **PRD reference(s):** §23.5 Edge Case #12; §25 T17
- **Module(s)/Screen(s):** ads
- **Story points:** 5 — MVP-mock suspension-signal handling; real Meta webhook handling is Phase 2's MADS-07.
- **Dependencies:** ADS-01, ADS-07
- **Testing tier(s):** unit, module (@ApplicationModuleTest), component test

## Sub-tasks
- [NEW] Backend: mocked suspension-signal handler → campaign status flag
- [NEW] Frontend: `useCampaignSuspension` hook (React Query for server data per RULES.md §7.1)
- [NEW] Frontend: `CampaignSuspensionBanner.tsx` component — all 5 PRD Part 21 states (default/loading/success/empty/error)
- [NEW] Frontend: component test (Testing Library, co-located, asserts on role/label per RULES.md §7.3)
