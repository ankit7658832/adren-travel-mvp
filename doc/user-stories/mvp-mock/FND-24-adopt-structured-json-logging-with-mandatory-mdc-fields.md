---
id: FND-24
epic: Foundation
phase: mock
status: not-started
story_points: 5
dependencies: ["FND-21"]
labels: ["backend", "foundation", "observability", "phase1"]
prd_references: ["§6.2"]
modules_or_screens: ["shared"]
testing_tiers: ["unit"]
---

# FND-24: Adopt structured JSON logging with mandatory MDC fields

## Summary (business)
This story ensures that the platform's internal system logs are written in a consistent, searchable format and always include key details like which consultant, currency, and market a log entry relates to. Since Adren operates across six countries and currencies, this is essential for support staff and engineers to quickly investigate issues and for maintaining financial and compliance traceability.

## User Story
**As a** on-call engineer, **I want** have every log line inside a request or listener scope carry traceId, consultantId, and (where relevant) currency/market, **so that** log aggregation/search is usable as the primary debugging tool across six jurisdictions and six currencies, per RULES.md §6.2.

## Acceptance Criteria
- Given any log line is written inside a request or listener scope, when it is emitted, then it is JSON-structured and carries `traceId` and `consultantId` at minimum.
- Given a log line concerns a monetary amount or a compliance-relevant calculation, when it is emitted, then it also carries `currency` and/or `market` — never a bare number or a market-less compliance line.

## Developer Notes
- **PRD reference(s):** §6.2 Structured logging standards (RULES.md)
- **Module(s)/Screen(s):** shared
- **Story points:** 5 — Logback structured-encoder config plus an MDC-population convention enforced at the filter/listener boundary established in FND-21.
- **Dependencies:** FND-21
- **Testing tier(s):** unit

## Sub-tasks
- [NEW] Backend: Logback structured JSON encoder configuration
- [EXTEND] Backend: MDC population (`traceId`, `consultantId`, `currency`, `market`) at the filter and listener boundary from FND-21
- [NEW] Backend: unit test — a sample monetary log line asserts `currency` is present
