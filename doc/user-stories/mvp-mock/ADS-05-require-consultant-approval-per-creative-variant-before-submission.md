---
id: ADS-05
epic: Ads/Campaign Management
phase: mock
status: not-started
story_points: 5
dependencies: ["ADS-04"]
labels: ["backend", "frontend", "ads", "phase1"]
prd_references: ["§14.2", "§21.8"]
modules_or_screens: ["ads", "Campaign Builder (21.8)"]
testing_tiers: ["unit", "module (@ApplicationModuleTest)", "component test"]
---

# ADS-05: Require Consultant approval per creative variant before submission

## Summary (business)
Before an ad campaign can be sent off for final review, the Consultant must personally look at and approve each ad option that will be used - they cannot skip this step. This ensures a human always signs off on what customers will actually see, protecting the brand's reputation.

## User Story
**As a** Consultant, **I want** approve each creative variant individually before the campaign is submitted for policy review, **so that** PRD §14.2 step 4's mandatory approval step is enforced, not skippable.

## Acceptance Criteria
- Given creative variants are displayed, when the Consultant attempts to submit for review without checking any approval checkbox, then submission is blocked — approval per variant is mandatory, not optional.

## Developer Notes
- **PRD reference(s):** §14.2 Flow step 4; §21.8 Campaign Builder (approval checkboxes)
- **Module(s)/Screen(s):** ads, Campaign Builder (21.8)
- **Story points:** 5 — Approval-gate enforcement on top of ADS-04's gallery, mirroring AI-06's human-in-the-loop pattern.
- **Dependencies:** ADS-04
- **Testing tier(s):** unit, module (@ApplicationModuleTest), component test

## Sub-tasks
- [EXTEND] Backend: `approveCreativeVariant` business logic / state-transition method
- [EXTEND] Backend: domain event publication (`@Transactional`, same method scope)
- [EXTEND] Backend: REST endpoint `POST /api/v1/campaigns/{id}/creative-variants/{variantId}/approval`
- [NEW] Backend: unit test
- [NEW] Backend: module/integration test
- [EXTEND] Frontend: `useCreativeApproval` hook (React Query for server data per RULES.md §7.1)
- [NEW] Frontend: `CreativeApprovalPanel.tsx` component — all 5 PRD Part 21 states (default/loading/success/empty/error)
- [NEW] Frontend: component test (Testing Library, co-located, asserts on role/label per RULES.md §7.3)
