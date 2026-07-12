---
id: LLM-08
epic: LLM Production Readiness
phase: production
status: not-started
story_points: 5
dependencies: ["AI-09", "LLM-02"]
labels: ["backend", "ai", "phase2"]
prd_references: ["§5"]
modules_or_screens: ["ai"]
testing_tiers: ["unit", "module (@ApplicationModuleTest)"]
---

# LLM-08: Tie AI suggestion caching invalidation to the rate-staleness signal at production scale

## Summary (business)
Any temporary storage of AI suggestions (to make the system faster) will be automatically cleared out as soon as the underlying pricing or availability data changes, rather than expiring on a fixed timer. This ensures customers never see AI recommendations based on outdated deals or prices, even as we optimize for speed.

## User Story
**As a** backend engineer, **I want** have any AI suggestion cache invalidate based on the same staleness signal that triggers re-validation, never a fixed TTL, **so that** backend-best-practices §5's explicit warning is honored if/when AI suggestion caching is introduced for production latency reasons.

## Acceptance Criteria
- Given an AI suggestion cache is introduced for latency reasons, when a cached suggestion's underlying rate goes stale, then the cache entry is invalidated by the same staleness signal AI-09's re-validation uses, never surviving past it on a fixed TTL alone.

## Developer Notes
- **PRD reference(s):** backend-best-practices skill §5 (Caching strategy)
- **Module(s)/Screen(s):** ai
- **Story points:** 5 — Caching-layer story explicitly scoped to avoid the exact anti-pattern backend-best-practices §5 warns against — only relevant once caching is actually introduced for latency.
- **Dependencies:** AI-09, LLM-02
- **Testing tier(s):** unit, module (@ApplicationModuleTest)

## Sub-tasks
- [NEW] Backend: AI suggestion cache keyed with a staleness-signal-bound TTL, not a fixed TTL
- [NEW] Backend: unit test — cache entry invalidated when the staleness signal fires before a fixed TTL would have expired it
