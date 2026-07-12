---
id: OBS-01
epic: Production Observability
phase: production
status: not-started
story_points: 5
dependencies: ["FND-21"]
labels: ["backend", "observability", "phase2"]
prd_references: ["§6.1"]
modules_or_screens: ["shared (observability)"]
testing_tiers: ["integration (Testcontainers)"]
---

# OBS-01: Wire OTel/Zipkin tracing export for production correlation IDs

## Summary (business)
When a customer request runs into trouble, support staff need to trace exactly what happened across every step of the system, not just guess. This story connects that tracking to a live monitoring tool in the production environment, so a single problem can be followed end-to-end while it's actually happening, cutting the time it takes to find and fix issues.

## User Story
**As a** on-call engineer, **I want** have FND-21's correlation-ID propagation exported to a real tracing backend in production, **so that** RULES.md §6.1's traceId propagation across the async event-listener boundary is queryable in production, not just verified by FND-21's local integrationTest.

## Acceptance Criteria
- Given a production request triggers an async event-listener chain, when the trace is exported, then the same traceId is queryable across the request span and every downstream listener span in the production tracing backend.

## Developer Notes
- **PRD reference(s):** §6.1 Correlation IDs (RULES.md)
- **Module(s)/Screen(s):** shared (observability)
- **Story points:** 5 — Exporter wiring on top of FND-21's already-built propagation mechanism — `spring-modulith-observability` is on the classpath but unconfigured per RULES.md's explicit reconciliation note.
- **Dependencies:** FND-21
- **Testing tier(s):** integration (Testcontainers)

## Sub-tasks
- [NEW] Backend: OTel/Zipkin exporter configuration in production `application.yml`
- [NEW] Backend: integrationTest — traceId queryable across the async boundary in the exported trace
