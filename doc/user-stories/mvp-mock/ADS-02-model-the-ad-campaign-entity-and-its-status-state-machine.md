---
id: ADS-02
epic: Ads/Campaign Management
phase: mock
status: not-started
story_points: 5
dependencies: ["ADS-01"]
labels: ["backend", "ads", "phase1"]
prd_references: ["§20.13"]
modules_or_screens: ["ads"]
testing_tiers: ["unit", "module (@ApplicationModuleTest)"]
---

# ADS-02: Model the Ad Campaign entity and its status state machine

## Summary (business)
Every ad campaign follows a fixed, predictable lifecycle - from awaiting approval, to compliance checks, to going live, to being paused or stopped - and the system will not allow a campaign to skip steps or jump into an invalid state. This protects the business from ads accidentally going live without the required approvals.

## User Story
**As a** backend engineer, **I want** have an `AdCampaign` entity enforcing the PendingApproval → PendingPolicyReview → Live → Paused/Rejected/SpendCapReached state machine, **so that** PRD §20.13's status enum is enforced as entity-owned transitions, not scattered service-layer conditionals, per backend-best-practices §1.

## Acceptance Criteria
- Given a campaign transitions from PendingApproval to Live, when an invalid transition is attempted instead (e.g. Rejected → Live), then the entity throws `IllegalStateException` rather than silently no-op'ing.

## Developer Notes
- **PRD reference(s):** §20.13 Ad Campaign
- **Module(s)/Screen(s):** ads
- **Story points:** 5 — State-machine entity design, mirroring `Itinerary.markAsQuotation()`'s established pattern.
- **Dependencies:** ADS-01
- **Testing tier(s):** unit, module (@ApplicationModuleTest)

## Sub-tasks
- [NEW] Backend: `AdCampaign` entity + `AdCampaignRepository` (package-private, own Flyway migration)
- [NEW] Backend: `createCampaign` on the module `ServiceImpl` + domain event publication (`@Transactional`)
- [NEW] Backend: REST endpoint `POST /api/v1/campaigns` (Controller depends on the module's `Api` interface only)
- [NEW] Backend: unit test (service logic, mocked repository/publisher)
- [NEW] Backend: `@ApplicationModuleTest` module-slice test (event publish/consume)
