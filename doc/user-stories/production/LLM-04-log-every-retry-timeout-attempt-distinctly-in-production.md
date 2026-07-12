---
id: LLM-04
epic: LLM Production Readiness
phase: production
status: not-started
story_points: 5
dependencies: ["AI-13", "LLM-02"]
labels: ["backend", "ai", "phase2"]
prd_references: ["§7", "§6.3"]
modules_or_screens: ["ai"]
testing_tiers: ["integration (Testcontainers)"]
---

# LLM-04: Log every retry/timeout attempt distinctly in production

## Summary (business)
Every time the system has to retry a request to the AI provider because of a slow or failed response, each attempt will be recorded separately in our records, not just the final successful one. This gives us a complete and trustworthy history for audits and for diagnosing problems, which matters more once we're handling real customer traffic instead of test scenarios.

## User Story
**As a** Super Admin, **I want** see each retried LLM call attempt as a distinct audit entry in production, not just the final successful one, **so that** backend-best-practices §7 and RULES.md §6.3's audit-completeness requirement holds under real production retry conditions, not just the MVP's synthetic test scenarios.

## Acceptance Criteria
- Given a production Groq/LLM call times out twice and succeeds on the third attempt, when the audit log is inspected, then three distinct attempt entries exist, not one entry representing only the final successful call.

## Developer Notes
- **PRD reference(s):** backend-best-practices skill §7; §6.3 (RULES.md)
- **Module(s)/Screen(s):** ai
- **Story points:** 5 — Extends AI-13's timeout/retry logic with production-scale audit granularity.
- **Dependencies:** AI-13, LLM-02
- **Testing tier(s):** integration (Testcontainers)

## Sub-tasks
- [EXTEND] Backend: per-attempt audit logging on every retry, in production configuration
- [NEW] Backend: integrationTest — 3-attempt scenario produces 3 audit entries
