---
id: SUP-06
epic: Supplier Live Integrations
phase: production
status: not-started
story_points: 5
dependencies: ["SUP-05"]
labels: ["backend", "supplier", "phase2"]
prd_references: ["§10.2.3"]
modules_or_screens: ["supplier"]
testing_tiers: ["integration (Testcontainers)"]
---

# SUP-06: Cut TBO over to production with UAT/prod credential separation

## Summary (business)
This switches our TBO hotel connection to production using dedicated live credentials that are kept completely separate from our testing credentials. This prevents test activity from ever touching real customer bookings, and keeps production access secure and auditable.

## User Story
**As a** Super Admin, **I want** have TBO run against production using dedicated production credentials, distinct from UAT, **so that** PRD §10.2.3's explicit UAT-vs-production credential separation requirement is met.

## Acceptance Criteria
- Given the application runs under a production profile, when TBO credentials resolve, then they are the production credential set, never the UAT set, sourced via FND-11's Secrets Manager pattern.

## Developer Notes
- **PRD reference(s):** §10.2.3 TBO (Authentication)
- **Module(s)/Screen(s):** supplier
- **Story points:** 5 — Credential-separation cutover; rate-limit tier confirmation is the remaining open item from sandbox testing.
- **Dependencies:** SUP-05
- **Testing tier(s):** integration (Testcontainers)

## Sub-tasks
- [EXTEND] Backend: production vs. UAT credential separation enforced via profile-scoped Secrets Manager entries
- [NEW] Backend: account-tier rate limit confirmed and configured
- [NEW] Backend: production-fixture-shaped integrationTest
