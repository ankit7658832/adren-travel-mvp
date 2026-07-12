---
id: SEC-05
epic: Security Hardening
phase: production
status: not-started
story_points: 5
dependencies: ["FND-22"]
labels: ["backend", "security", "phase2"]
prd_references: ["§5.4"]
modules_or_screens: ["shared (security)"]
testing_tiers: ["unit"]
---

# SEC-05: Audit every request DTO for mass-assignment exposure

## Summary (business)
This story reviews every form of incoming data across the platform to make sure users can only fill in the fields they're supposed to, and can't sneak in extra hidden values to change things like account status or internal flags they shouldn't have access to. It closes a common way attackers escalate their own privileges or tamper with records they don't own.

## User Story
**As a** platform security owner, **I want** have every request DTO audited to confirm it binds explicit fields onto entities via business methods, never raw entity binding, **so that** RULES.md §5.4's mass-assignment concern is verified across every endpoint that shipped during Phase 1, not just the one it was originally called out on.

## Acceptance Criteria
- Given every Phase 1 request-body-accepting endpoint is audited, when the audit runs, then each one binds onto a request DTO mapped explicitly onto the entity's business constructor/methods — no endpoint allows a client to set `status`, `ai_generated`, or any other entity-internal field via extra JSON fields.

## Developer Notes
- **PRD reference(s):** §5.4 OWASP-relevant concerns (RULES.md, mass assignment)
- **Module(s)/Screen(s):** shared (security)
- **Story points:** 5 — Audit + remediation pass across the full endpoint surface built in Phase 1 — breadth-bound, not individually complex.
- **Dependencies:** FND-22
- **Testing tier(s):** unit

## Sub-tasks
- [NEW] Backend: mass-assignment audit checklist run against every Phase 1 endpoint
- [NEW] Backend: remediation for any endpoint found binding raw entities
- [NEW] Backend: unit test per remediated endpoint
