---
id: SUP-08
epic: Supplier Live Integrations
phase: production
status: not-started
story_points: 5
dependencies: ["SUP-07"]
labels: ["backend", "supplier", "phase2"]
prd_references: ["§10.2.4"]
modules_or_screens: ["supplier"]
testing_tiers: ["integration (Testcontainers)"]
---

# SUP-08: Cut Mystifly over to production with a dedicated rate-limit bucket

## Summary (business)
This moves our Mystifly flight connection to production with its own dedicated capacity allowance, separate from our hotel suppliers. Because flights need to be re-searched much more often than hotels (fares expire quickly), this ensures flight search stays fast and reliable even during high demand, without competing with hotel traffic.

## User Story
**As a** Super Admin, **I want** have Mystifly run against production with its own dedicated rate-limit bucket, separate from hotel suppliers, **so that** PRD §10.2.4's explicit requirement (flights are the most frequently re-searched product given fast fare expiry) is met in production, per backend-best-practices §3.

## Acceptance Criteria
- Given Mystifly and the hotel suppliers all receive concurrent production traffic, when rate limiting is evaluated, then Mystifly's per-minute search cap is enforced from its own dedicated bucket, never sharing budget with Hotelbeds/STUBA/TBO.

## Developer Notes
- **PRD reference(s):** §10.2.4 Mystifly (Rate limits)
- **Module(s)/Screen(s):** supplier
- **Story points:** 5 — Narrower cutover focused specifically on the dedicated-bucket requirement §10.2.4 calls out by name.
- **Dependencies:** SUP-07
- **Testing tier(s):** integration (Testcontainers)

## Sub-tasks
- [EXTEND] Backend: dedicated production rate-limit bucket for Mystifly, isolated from hotel-supplier buckets
- [NEW] Backend: production-fixture-shaped integrationTest
