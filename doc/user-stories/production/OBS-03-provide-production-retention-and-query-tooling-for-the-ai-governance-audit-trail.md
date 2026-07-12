---
id: OBS-03
epic: Production Observability
phase: production
status: not-started
story_points: 5
dependencies: ["AI-07", "AI-08"]
labels: ["backend", "observability", "ai", "phase2"]
prd_references: ["§6.3"]
modules_or_screens: ["ai (observability)"]
testing_tiers: ["integration (Testcontainers)"]
---

# OBS-03: Provide production retention and query tooling for the AI governance audit trail

## Summary (business)
Every AI-generated recommendation shown to a consultant is recorded for compliance purposes, and those records need to be kept and searchable for much longer than everyday system logs, which are regularly cleared out. This story makes sure compliance staff can look up any past AI recommendation on demand, even if it's many months old, satisfying audit and regulatory needs.

## User Story
**As a** Super Admin/compliance owner, **I want** query the AI suggestion audit trail (AI-07/AI-08) in production with retention independent of the application-log window, **so that** RULES.md §6.3's explicit distinction — audit trail retention/immutability differs from sampled/rotated application logs — is honored operationally, not just architecturally.

## Acceptance Criteria
- Given a compliance review needs an AI suggestion from 6 months prior, when a query is run against the production audit-trail store, then the record is retrievable, unaffected by the application-log aggregator's shorter retention window.

## Developer Notes
- **PRD reference(s):** §6.3 (RULES.md, AI governance audit)
- **Module(s)/Screen(s):** ai (observability)
- **Story points:** 5 — Production retention/query-tooling story on top of AI-07/AI-08's already-built insert-only audit table.
- **Dependencies:** AI-07, AI-08
- **Testing tier(s):** integration (Testcontainers)

## Sub-tasks
- [NEW] Backend: production retention policy for `ai_suggestion_audit_log`, independent of app-log retention
- [NEW] Backend: query tooling/interface for compliance review
- [NEW] Backend: integrationTest — retrieval beyond the app-log retention window
