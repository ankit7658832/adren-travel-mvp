---
id: SEC-06
epic: Security Hardening
phase: production
status: not-started
story_points: 3
dependencies: ["FND-08"]
labels: ["backend", "security", "whitelabel", "phase2"]
prd_references: ["§5.4"]
modules_or_screens: ["whitelabel"]
testing_tiers: ["integration (Testcontainers)"]
---

# SEC-06: Harden the dynamic per-Consultant CORS allow-list for production

## Summary (business)
This story tests the system that decides which partner websites are allowed to talk to Adren's platform, specifically checking what happens when a consultant removes or changes their custom domain. It ensures that once a partner's website is no longer authorized, it immediately loses access rather than remaining trusted due to outdated records.

## User Story
**As a** platform security owner, **I want** have FND-08's dynamic CORS allow-list verified against production domain-mapping edge cases (domain removal, re-mapping, expired CNAME), **so that** the MVP's dynamic CORS mechanism holds under real domain-lifecycle churn, not just the happy-path mapping FND-08 tested.

## Acceptance Criteria
- Given a Consultant's CNAME domain is unmapped or reassigned, when a request from the old domain arrives, then CORS immediately reflects the current domain registry state — no stale allow-list entry persists past the unmapping.

## Developer Notes
- **PRD reference(s):** §5.4 OWASP-relevant concerns (RULES.md, CORS)
- **Module(s)/Screen(s):** whitelabel
- **Story points:** 3 — Edge-case hardening pass on top of FND-08's already-built dynamic allow-list mechanism.
- **Dependencies:** FND-08
- **Testing tier(s):** integration (Testcontainers)

## Sub-tasks
- [EXTEND] Backend: CORS allow-list cache invalidation on domain unmapping/remapping
- [NEW] Backend: integrationTest — unmapping edge case
