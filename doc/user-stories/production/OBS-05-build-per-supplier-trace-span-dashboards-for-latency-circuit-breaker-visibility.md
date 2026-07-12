---
id: OBS-05
epic: Production Observability
phase: production
status: not-started
story_points: 5
dependencies: ["OBS-01"]
labels: ["backend", "observability", "supplier", "phase2"]
prd_references: ["§6.3", "§24.2"]
modules_or_screens: ["supplier (observability)"]
testing_tiers: ["integration (Testcontainers)"]
---

# OBS-05: Build per-supplier trace-span dashboards for latency/circuit-breaker visibility

## Summary (business)
When a search request goes out to all the travel suppliers (hotel, flight, and activity providers) at once, a slowdown from just one of them can currently get lost in a combined view. This story gives the operations team a dashboard that shows each supplier's speed and health separately, so a poorly performing supplier can be spotted and addressed immediately instead of dragging down the whole booking experience unnoticed.

## User Story
**As a** on-call engineer, **I want** see each supplier's (Hotelbeds/STUBA/TBO/Mystifly/Transferz/Widgety/HBActivities) latency and circuit-breaker state individually, not collapsed into one aggregate search span, **so that** RULES.md §6.3's explicit per-supplier trace-span requirement is realized as an actual dashboard, feeding SUP-13's circuit-breaker tuning.

## Acceptance Criteria
- Given a single search request fans out to all seven live suppliers in parallel, when the trace is inspected, then each supplier's latency/error is visible as its own distinct span on the dashboard, with one slow supplier clearly identifiable rather than hidden in an aggregate 'search' span.

## Developer Notes
- **PRD reference(s):** §6.3 (RULES.md, per-supplier trace spans); §24.2 NFR Supplier Integration
- **Module(s)/Screen(s):** supplier (observability)
- **Story points:** 5 — Dashboard built on top of OBS-01's tracing export, specifically surfacing the per-supplier span granularity RULES.md §6.3 calls out.
- **Dependencies:** OBS-01
- **Testing tier(s):** integration (Testcontainers)

## Sub-tasks
- [NEW] Backend: per-supplier trace-span dashboard
- [NEW] Backend: integrationTest — one distinct span per supplier in a fan-out search
