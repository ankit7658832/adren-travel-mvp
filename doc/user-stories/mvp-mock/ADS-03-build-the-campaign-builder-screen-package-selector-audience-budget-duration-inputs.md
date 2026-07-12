---
id: ADS-03
epic: Ads/Campaign Management
phase: mock
status: not-started
story_points: 8
dependencies: ["ADS-02", "BOK-12", "FES-08"]
labels: ["backend", "frontend", "ads", "phase1"]
prd_references: ["§14.2", "§21.8"]
modules_or_screens: ["ads", "Campaign Builder (21.8) — NEW feature folder"]
testing_tiers: ["unit", "module (@ApplicationModuleTest)", "component test"]
---

# ADS-03: Build the Campaign Builder screen — package selector, audience/budget/duration inputs

## Summary (business)
Consultants get a simple screen where they pick one of their published travel packages and set who should see the ad, how much to spend, and for how long, directly from the package they're promoting. This makes it fast and low-effort for Consultants to turn a travel package into an ad campaign.

## User Story
**As a** Consultant, **I want** select a published Package and provide audience, budget, and duration inputs to start a campaign, **so that** PRD §14.2 steps 1–2 and §21.8's layout are implemented.

## Acceptance Criteria
- Given a Consultant opts into 'Promote this Package' from the Package Builder, when the Campaign Builder opens, then the selected Package is pre-populated and audience/budget/duration fields are required before proceeding.

## Developer Notes
- **PRD reference(s):** §14.2 Flow steps 1-2; §21.8 Campaign Builder
- **Module(s)/Screen(s):** ads, Campaign Builder (21.8) — NEW feature folder
- **Story points:** 8 — New multi-step screen; form/validation library (FES-08) is a direct dependency given the multi-field campaign-input form.
- **Dependencies:** ADS-02, BOK-12, FES-08
- **Testing tier(s):** unit, module (@ApplicationModuleTest), component test

## Sub-tasks
- [EXTEND] Backend: `submitCampaignInputs` business logic / state-transition method
- [EXTEND] Backend: domain event publication (`@Transactional`, same method scope)
- [EXTEND] Backend: REST endpoint `POST /api/v1/campaigns/{id}/inputs`
- [NEW] Backend: unit test
- [NEW] Backend: module/integration test
- [NEW] Frontend: `useCampaignBuilder` hook (React Query for server data per RULES.md §7.1)
- [NEW] Frontend: `CampaignBuilder.tsx` component — all 5 PRD Part 21 states (default/loading/success/empty/error)
- [NEW] Frontend: component test (Testing Library, co-located, asserts on role/label per RULES.md §7.3)
