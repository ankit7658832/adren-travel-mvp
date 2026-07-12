---
id: SUP-02
epic: Supplier Live Integrations
phase: production
status: not-started
story_points: 8
dependencies: ["SUP-01"]
labels: ["backend", "supplier", "phase2"]
prd_references: ["§10.2.1"]
modules_or_screens: ["supplier"]
testing_tiers: ["integration (Testcontainers)"]
---

# SUP-02: Cut Hotelbeds over to production with contract-tier rate limits and nightly content sync

## Summary (business)
This switches our Hotelbeds hotel connection over to the real, live production system with the call volume and speed we've contracted for, and keeps hotel photos, descriptions, and amenities automatically refreshed overnight. It means customers get accurate, up-to-date hotel information and reliable search performance once we're live with real bookings.

## User Story
**As a** Super Admin, **I want** have Hotelbeds run against production with the contracted rate-limit tier and a working nightly Content API sync, **so that** PRD §10.2.1's production sync-frequency and rate-limit requirements are fully live, not just sandbox-verified.

## Acceptance Criteria
- Given Hotelbeds' contracted per-second call cap is approached in production, when the token-bucket limiter engages, then overflow requests queue with backoff rather than being dropped.
- Given the nightly Content API batch job runs in production, when it completes, then static content (images, descriptions, amenities) refreshes without affecting real-time search latency.

## Developer Notes
- **PRD reference(s):** §10.2.1 Hotelbeds (Rate limits, Sync frequency)
- **Module(s)/Screen(s):** supplier
- **Story points:** 8 — Production cutover carries contract-tier configuration and IP whitelisting risk beyond what sandbox testing can fully surface.
- **Dependencies:** SUP-01
- **Testing tier(s):** integration (Testcontainers)

## Sub-tasks
- [EXTEND] Backend: production credential + IP whitelist configuration (via FND-11's Secrets Manager pattern)
- [EXTEND] Backend: token-bucket limiter tuned to the contracted production tier
- [NEW] Backend: production-fixture-shaped integrationTest (per TST-06's CI separation)
- [NEW] Backend: production nightly Content API sync job verified against real data
