---
id: SEC-08
epic: Security Hardening
phase: production
status: not-started
story_points: 5
dependencies: ["FND-24"]
labels: ["backend", "security", "observability", "phase2"]
prd_references: ["§6.2"]
modules_or_screens: ["shared (security)"]
testing_tiers: ["unit"]
---

# SEC-08: Audit secrets/credential/PII log redaction across all modules

## Summary (business)
This story checks every part of the system's internal activity logs to make sure sensitive information — like passwords, API keys, payment details, or customers' personal information — is never accidentally written into logs where engineers or support staff could see it. This reduces the risk of a data leak through internal tooling rather than an external attack.

## User Story
**As a** platform security owner, **I want** have every module's logging audited to confirm no secret, token, or full PII value is ever logged, **so that** RULES.md §6.2's redaction rule is verified across the full module set that shipped in Phase 1, not just spot-checked.

## Acceptance Criteria
- Given the log-redaction audit runs across every module, when log statements are reviewed, then no supplier/BYOS/Meta credential, payment token, or full traveler PII value appears in any log line or exception message — masking is applied wherever a request/response body might be logged.

## Developer Notes
- **PRD reference(s):** §6.2 Structured logging standards (RULES.md)
- **Module(s)/Screen(s):** shared (security)
- **Story points:** 5 — Audit + remediation pass, structurally similar to SEC-05 but targeting logging rather than request binding.
- **Dependencies:** FND-24
- **Testing tier(s):** unit

## Sub-tasks
- [NEW] Backend: log-redaction audit checklist run against every module
- [NEW] Backend: remediation for any unmasked secret/PII log statement found
- [NEW] Backend: unit test per remediated log statement
