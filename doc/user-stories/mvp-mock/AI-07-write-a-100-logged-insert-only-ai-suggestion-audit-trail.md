---
id: AI-07
epic: AI Layer
phase: mock
status: not-started
story_points: 5
dependencies: ["AI-02"]
labels: ["backend", "ai", "phase1"]
prd_references: ["§11.2", "§24.3", "§6.3"]
modules_or_screens: ["ai"]
testing_tiers: ["unit", "module (@ApplicationModuleTest)", "integration (Testcontainers)"]
---

# AI-07: Write a 100%-logged, insert-only AI suggestion audit trail

## Summary (business)
Every single AI suggestion made by the system is permanently recorded - what was asked, what data it used, what it suggested, and what happened to that suggestion - with no exceptions or spot-checking. This creates a complete, trustworthy record for oversight, dispute resolution, and regulatory compliance, and if that record can't be saved, the suggestion is not allowed to be used.

## User Story
**As a** Super Admin, **I want** have every single AI suggestion logged with its input, source data, output, and disposition — no sampling, **so that** PRD §11.2 principle 5, §24.3's 100%-logged NFR, and RULES.md §6.3's audit-trail requirement are satisfied.

## Acceptance Criteria
- Given any AI suggestion is generated, when the audit-log write is attempted, then if the write fails, the suggestion is not usable/displayed — the audit write is a transactional gate, not a fire-and-forget side channel (backend-best-practices §7).
- Given 100 AI calls are made in a load test, when the audit log is inspected, then exactly 100 entries exist — zero sampling.

## Developer Notes
- **PRD reference(s):** §11.2 principle 5; §24.3 NFR AI Governance; §6.3 (RULES.md)
- **Module(s)/Screen(s):** ai
- **Story points:** 5 — Dedicated insert-only `ai_suggestion_audit_log` table distinct from application logs, per RULES.md §6.3's explicit retention/immutability distinction.
- **Dependencies:** AI-02
- **Testing tier(s):** unit, module (@ApplicationModuleTest), integration (Testcontainers)

## Sub-tasks
- [NEW] Backend: `AiSuggestionAuditLog` entity (insert-only)
- [EXTEND] Backend: every AI capability writes the audit log transactionally before returning a usable suggestion
- [NEW] Backend: unit test — failed audit write blocks suggestion use
- [NEW] Backend: integrationTest — 100% logging under concurrent calls
