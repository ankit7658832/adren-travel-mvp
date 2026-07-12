---
id: SEC-02
epic: Security Hardening
phase: production
status: not-started
story_points: 8
dependencies: ["FND-03", "FND-08", "SEC-03", "SEC-04", "SEC-05"]
labels: ["backend", "security", "phase2"]
prd_references: ["§5.4"]
modules_or_screens: ["shared (security)"]
testing_tiers: ["integration (Testcontainers)"]
---

# SEC-02: Remediate findings from a full OWASP API Top 10 penetration test pass

## Summary (business)
This story pays for an independent security expert to try to break into the platform using a standard industry checklist of common attack methods, and requires every serious problem they find to be fixed before the product goes live. It gives the business confidence that the platform's defenses actually work in practice, not just on paper.

## User Story
**As a** platform security owner, **I want** have every OWASP API Top 10 category assessed and remediated before GA, **so that** RULES.md §5.4's OWASP-relevant concerns are verified by an actual penetration test, not just the design-time mitigations already built (FND-03, FND-08, SEC-03, SEC-04, SEC-05).

## Acceptance Criteria
- Given the penetration test completes, when findings are reviewed, then every finding is triaged, and all Critical/High findings are remediated before GA sign-off.

## Developer Notes
- **PRD reference(s):** §5.4 OWASP-relevant concerns (RULES.md)
- **Module(s)/Screen(s):** shared (security)
- **Story points:** 8 — Scope depends entirely on pentest findings — sized as the upper bound for a full-platform OWASP Top 10 pass with real remediation work, not just the test itself.
- **Dependencies:** FND-03, FND-08, SEC-03, SEC-04, SEC-05
- **Testing tier(s):** integration (Testcontainers)

## Sub-tasks
- [NEW] Backend: engage penetration test against the full OWASP API Top 10 checklist
- [NEW] Backend: remediate all Critical/High findings
- [NEW] Backend: regression test per remediated finding
