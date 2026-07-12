---
id: ADS-06
epic: Ads/Campaign Management
phase: mock
status: not-started
story_points: 5
dependencies: ["ADS-05", "FND-02"]
labels: ["backend", "frontend", "ads", "phase1"]
prd_references: ["§14.2", "§20.13"]
modules_or_screens: ["ads", "Super Admin Console (21.6)"]
testing_tiers: ["unit", "module (@ApplicationModuleTest)", "component test"]
---

# ADS-06: Route approved campaigns through Super Admin brand-safety/policy review

## Summary (business)
Once a Consultant approves their ad content, it doesn't go live immediately - a Super Admin first checks it for brand safety and policy compliance, and can approve or reject it with a reason the Consultant can see. This extra checkpoint protects Adren's brand and ensures all advertising meets company and platform standards before money is spent.

## User Story
**As a** Super Admin, **I want** review a Consultant-approved campaign for brand-safety and policy compliance before it launches, **so that** PRD §14.2 step 5's review gate is implemented, transitioning the campaign to PendingPolicyReview.

## Acceptance Criteria
- Given a campaign has all creative variants Consultant-approved, when it is submitted, then its status transitions to PendingPolicyReview and appears in the Super Admin's review queue.
- Given Super Admin rejects the campaign, when the rejection is submitted, then status transitions to Rejected with a reason visible to the Consultant.

## Developer Notes
- **PRD reference(s):** §14.2 Flow step 5; §20.13 status enum (PendingPolicyReview)
- **Module(s)/Screen(s):** ads, Super Admin Console (21.6)
- **Story points:** 5 — Second state-machine transition + Super Admin queue UI on top of ADS-02/ADS-05.
- **Dependencies:** ADS-05, FND-02
- **Testing tier(s):** unit, module (@ApplicationModuleTest), component test

## Sub-tasks
- [EXTEND] Backend: `reviewCampaign` business logic / state-transition method
- [EXTEND] Backend: domain event publication (`@Transactional`, same method scope)
- [EXTEND] Backend: REST endpoint `POST /api/v1/campaigns/{id}/policy-review`
- [NEW] Backend: unit test
- [NEW] Backend: module/integration test
- [NEW] Frontend: `useCampaignPolicyReview` hook (React Query for server data per RULES.md §7.1)
- [NEW] Frontend: `CampaignPolicyReviewQueue.tsx` component — all 5 PRD Part 21 states (default/loading/success/empty/error)
- [NEW] Frontend: component test (Testing Library, co-located, asserts on role/label per RULES.md §7.3)
