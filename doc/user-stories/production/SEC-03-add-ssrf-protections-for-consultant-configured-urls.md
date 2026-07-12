---
id: SEC-03
epic: Security Hardening
phase: production
status: not-started
story_points: 5
dependencies: ["FND-06"]
labels: ["backend", "security", "phase2"]
prd_references: ["§5.4"]
modules_or_screens: ["whitelabel", "supplier"]
testing_tiers: ["unit", "integration (Testcontainers)"]
---

# SEC-03: Add SSRF protections for Consultant-configured URLs

## Summary (business)
This story stops a malicious travel consultant (a customer who manages their own branded storefront on the platform) from tricking the system into calling internal, private company systems by disguising the request as an innocent web link. Without this protection, an attacker could potentially steal internal cloud credentials or access systems that should never be reachable from the outside.

## User Story
**As a** platform security owner, **I want** have any Consultant-supplied URL (webhook URLs, whitelabel domain verification, BYOS base-URL overrides) validated against SSRF, **so that** RULES.md §5.4's explicit SSRF concern — including the AWS metadata endpoint `169.254.169.254` — is closed before any such input surface ships for real.

## Acceptance Criteria
- Given a Consultant supplies a URL that resolves to an internal/link-local address (e.g. `169.254.169.254`), when the backend validates it before fetching, then the request is rejected — an allow-list of expected external hosts is checked, not a deny-list of forbidden ones.

## Developer Notes
- **PRD reference(s):** §5.4 OWASP-relevant concerns (RULES.md, SSRF)
- **Module(s)/Screen(s):** whitelabel, supplier
- **Story points:** 5 — Focused validation-layer story covering every current and near-term Consultant-supplied-URL surface.
- **Dependencies:** FND-06
- **Testing tier(s):** unit, integration (Testcontainers)

## Sub-tasks
- [NEW] Backend: SSRF validation helper (allow-list based) applied to every Consultant-supplied URL input
- [NEW] Backend: unit test — internal/link-local address rejected
- [NEW] Backend: integrationTest
