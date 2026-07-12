---
id: OBS-02
epic: Production Observability
phase: production
status: not-started
story_points: 5
dependencies: ["FND-24"]
labels: ["backend", "observability", "phase2"]
prd_references: ["§6.2"]
modules_or_screens: ["shared (observability)"]
testing_tiers: ["integration (Testcontainers)"]
---

# OBS-02: Ship structured JSON logs to production log aggregation with a retention policy

## Summary (business)
The system already records detailed activity logs, but today they only exist locally and aren't easy to search in the live environment. This story ships those logs to a central, searchable system with a set retention period, so support staff can quickly investigate problems across all six countries the business operates in, rather than relying on scattered records.

## User Story
**As a** on-call engineer, **I want** have FND-24's structured JSON logs shipped to a production log aggregator with a defined retention policy, **so that** log aggregation/search is actually usable as the primary debugging tool in production across six jurisdictions, not just structured locally.

## Acceptance Criteria
- Given a production log line is emitted, when it is shipped, then it lands in the log aggregator with all mandatory MDC fields (traceId, consultantId, currency/market where applicable) intact, retained per the defined policy.

## Developer Notes
- **PRD reference(s):** §6.2 Structured logging standards (RULES.md)
- **Module(s)/Screen(s):** shared (observability)
- **Story points:** 5 — Shipping/retention infra on top of FND-24's already-built structured logging.
- **Dependencies:** FND-24
- **Testing tier(s):** integration (Testcontainers)

## Sub-tasks
- [NEW] Infra: production log-shipping pipeline with a defined retention policy
- [NEW] Infra: README / RULES.md documentation update
- [NEW] Infra: verification step (local run and/or CI check)
