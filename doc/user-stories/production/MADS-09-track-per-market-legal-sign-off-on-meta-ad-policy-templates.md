---
id: MADS-09
epic: Meta Ads API Real Integration
phase: production
status: not-started
story_points: 3
dependencies: ["MADS-02"]
labels: ["backend", "ads", "compliance", "phase2"]
prd_references: ["§19"]
modules_or_screens: ["ads", "compliance"]
testing_tiers: ["unit", "module (@ApplicationModuleTest)"]
---

# MADS-09: Track per-market legal sign-off on Meta ad policy templates

## Summary (business)
Before ads can run in a given country or region, Adren needs confirmation that its legal team has reviewed and approved the ad templates for that market's rules. This story makes sure that approval is tracked and that campaigns are held back from launching in a market until that legal sign-off is confirmed.

## User Story
**As a** Super Admin, **I want** know which markets' ad creative templates have completed legal sign-off before campaigns launch there, **so that** PRD §19's open item — whether the Ads module needs per-market legal sign-off on templates — is resolved and tracked operationally.

## Acceptance Criteria
- Given a campaign targets a market whose ad-template legal sign-off is not yet recorded, when launch is attempted, then the system blocks launch (or flags for manual override) until sign-off is recorded for that market.

## Developer Notes
- **PRD reference(s):** §19 Open Items for Business Confirmation
- **Module(s)/Screen(s):** ads, compliance
- **Story points:** 3 — Tracking/gating mechanism; the actual legal review itself is a business process outside engineering scope.
- **Dependencies:** MADS-02
- **Testing tier(s):** unit, module (@ApplicationModuleTest)

> ⚠️ **NEEDS CLARIFICATION:** PRD §19: whether the Ads module needs per-market legal sign-off on templates is an open item for business confirmation — this story implements the tracking/gating mechanism assuming sign-off is required; remove the gate if business confirms it is not needed.

## Sub-tasks
- [NEW] Backend: per-market ad-template legal-signoff flag + launch gate
- [NEW] Backend: unit test
