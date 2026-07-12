---
id: FND-07
epic: Foundation
phase: mock
status: not-started
story_points: 5
dependencies: ["FND-06"]
labels: ["backend", "foundation", "whitelabel", "phase1"]
prd_references: ["§24.5"]
modules_or_screens: ["whitelabel"]
testing_tiers: ["module (@ApplicationModuleTest)", "integration (Testcontainers)"]
---

# FND-07: Propagate branding/domain changes to the live storefront without a redeploy

## Summary (business)
This story ensures that when a consultant's branding or web address is updated, the change appears on their live site almost immediately, without requiring a full platform update. This means consultants don't have to wait on the engineering team just to see their own logo or color change go live.

## User Story
**As a** Consultant, **I want** see my branding and domain changes reflected on my live storefront within a short window, **so that** PRD §24.5's NFR is met and I don't have to wait for a platform release to see my own branding update.

## Acceptance Criteria
- Given Super Admin saves a branding change for a Consultant, when the change is persisted, then the Consultant's storefront reflects it within the NFR's defined short window without a backend redeploy.

## Developer Notes
- **PRD reference(s):** §24.5 NFR White-Label & Admin
- **Module(s)/Screen(s):** whitelabel
- **Story points:** 5 — Requires a cache-invalidation/propagation mechanism (e.g. short-TTL cache + event) rather than a full redeploy path.
- **Dependencies:** FND-06
- **Testing tier(s):** module (@ApplicationModuleTest), integration (Testcontainers)

## Sub-tasks
- [NEW] Infra: short-TTL branding cache + invalidation event on save
- [NEW] Infra: README / RULES.md documentation update
- [NEW] Infra: verification step (local run and/or CI check)
- [NEW] Backend: unit test for cache invalidation trigger
