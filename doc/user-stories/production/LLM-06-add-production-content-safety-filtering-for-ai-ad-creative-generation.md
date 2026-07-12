---
id: LLM-06
epic: LLM Production Readiness
phase: production
status: not-started
story_points: 5
dependencies: ["AI-12", "LLM-02"]
labels: ["backend", "ai", "ads", "phase2"]
prd_references: ["§14.4"]
modules_or_screens: ["ai", "ads"]
testing_tiers: ["unit", "module (@ApplicationModuleTest)"]
---

# LLM-06: Add production content-safety filtering for AI ad-creative generation

## Summary (business)
Before any AI-generated advertisement image or copy is shown to a Consultant for approval, it will automatically be screened to catch anything that would violate Meta's (Facebook/Instagram's) advertising rules. This avoids wasted ad spend and account penalties that would come from having ads rejected or flagged after the fact.

## User Story
**As a** Super Admin, **I want** have AI-generated ad creative pass a content-safety filter before it reaches the Consultant approval step, **so that** Meta policy compliance risk on AI-generated creative (PRD §14.4) is reduced before real ad spend is at stake in production.

## Acceptance Criteria
- Given AI generates a creative variant that would violate Meta's advertising content policy, when the safety filter runs, then the variant is excluded from the gallery before it ever reaches Consultant approval (ADS-05), rather than being caught only at Meta's own review.

## Developer Notes
- **PRD reference(s):** §14.4 AI Creative Generation
- **Module(s)/Screen(s):** ai, ads
- **Story points:** 5 — Content-safety filter layered onto AI-12's already-built creative-generation path.
- **Dependencies:** AI-12, LLM-02
- **Testing tier(s):** unit, module (@ApplicationModuleTest)

## Sub-tasks
- [NEW] Backend: content-safety filter service (policy rule set + provider-level moderation check)
- [EXTEND] Backend: filter applied before creative variants are persisted to `AdCampaign.creative_variants[]`
- [NEW] Backend: unit test
- [NEW] Backend: module test
