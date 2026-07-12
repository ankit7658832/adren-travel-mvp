---
id: FND-21
epic: Foundation
phase: mock
status: not-started
story_points: 5
dependencies: ["FND-01"]
labels: ["backend", "foundation", "observability", "phase1"]
prd_references: ["§6.1"]
modules_or_screens: ["shared", "notification"]
testing_tiers: ["unit", "integration (Testcontainers)"]
---

# FND-21: Propagate a correlation ID (traceId) across the async event-listener boundary

## Summary (business)
This story ensures that when the system processes a booking behind the scenes (for example, sending a confirmation notification after a booking completes), the technical logs from that background process can still be linked back to the original customer request. This makes it much faster for engineers to diagnose problems in multi-step processes like booking confirmations.

## User Story
**As a** on-call engineer, **I want** see the same traceId in a request's log line and in the async listener log lines it triggers, **so that** an event-driven flow (e.g. booking confirmation → notification) remains traceable end-to-end, per RULES.md §6.1 — today MDC context does not cross `@ApplicationModuleListener`'s async thread hop at all.

## Acceptance Criteria
- Given a request generates a traceId at the edge and triggers a domain event, when an `@ApplicationModuleListener` handles it on a different thread, then the same traceId appears in that listener's log lines.
- Given an error response is returned, when the client inspects it, then its `traceId` matches the server-side log lines for that request (RFC 7807 shape, FND-22).

## Developer Notes
- **PRD reference(s):** §6.1 Correlation IDs (RULES.md, reconciliation item #4)
- **Module(s)/Screen(s):** shared, notification
- **Story points:** 5 — Requires `TaskDecorator`-based MDC propagation across Spring's async executor — a known non-trivial async/observability pattern, explicitly flagged as needed before the first real listener body ships.
- **Dependencies:** FND-01
- **Testing tier(s):** unit, integration (Testcontainers)

## Sub-tasks
- [NEW] Backend: servlet filter generating traceId at the edge
- [NEW] Backend: `TaskDecorator` propagating MDC across the `@Async` executor backing `@ApplicationModuleListener`
- [NEW] Backend: unit test — decorator propagates context
- [NEW] Backend: integrationTest asserting identical traceId in request log and listener log for one real flow
