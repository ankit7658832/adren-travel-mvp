---
id: ADS-04
epic: Ads/Campaign Management
phase: mock
status: not-started
story_points: 5
dependencies: ["AI-12", "ADS-03"]
labels: ["backend", "frontend", "ads", "phase1"]
prd_references: ["§14.2", "§21.8"]
modules_or_screens: ["ads", "ai", "Campaign Builder (21.8)"]
testing_tiers: ["unit", "module (@ApplicationModuleTest)", "component test"]
---

# ADS-04: Generate and display AI creative variant gallery in the Campaign Builder

## Summary (business)
After a Consultant sets up a campaign, the system automatically generates several ready-to-use ad options (different images and wording) based on the real package details and current price. This saves Consultants time and ensures the ads are always accurate and on-brand, rather than requiring them to design ads from scratch.

## User Story
**As a** Consultant, **I want** see multiple AI-generated creative variants (image/copy combinations) for my campaign, **so that** PRD §14.2 step 3 and §21.8's creative-variant-gallery layout are implemented.

## Acceptance Criteria
- Given a Campaign Builder session reaches the creative step, when AI-12's generation runs, then multiple image/copy variant combinations are displayed in a gallery, each grounded in the Package's actual content and live price.

## Developer Notes
- **PRD reference(s):** §14.2 Flow step 3; §21.8 Campaign Builder
- **Module(s)/Screen(s):** ads, ai, Campaign Builder (21.8)
- **Story points:** 5 — Frontend consumer of AI-12's already-built generation capability, plus the `creative_variants[]` persistence on `AdCampaign`.
- **Dependencies:** AI-12, ADS-03
- **Testing tier(s):** unit, module (@ApplicationModuleTest), component test

## Sub-tasks
- [EXTEND] Backend: `creative_variants[]` persisted on `AdCampaign` from AI-12's output
- [EXTEND] Frontend: `useCreativeVariants` hook (React Query for server data per RULES.md §7.1)
- [NEW] Frontend: `CreativeVariantGallery.tsx` component — all 5 PRD Part 21 states (default/loading/success/empty/error)
- [NEW] Frontend: component test (Testing Library, co-located, asserts on role/label per RULES.md §7.3)
