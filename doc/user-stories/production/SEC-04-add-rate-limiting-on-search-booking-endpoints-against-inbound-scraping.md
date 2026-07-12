---
id: SEC-04
epic: Security Hardening
phase: production
status: not-started
story_points: 5
dependencies: ["FND-13"]
labels: ["backend", "security", "phase2"]
prd_references: ["§5.4"]
modules_or_screens: ["booking"]
testing_tiers: ["unit", "integration (Testcontainers)"]
---

# SEC-04: Add rate limiting on search/booking endpoints against inbound scraping

## Summary (business)
This story adds a speed limit on how many searches or bookings any single user can make in a short period of time. It prevents a competitor or bad actor from rapidly copying Adren's negotiated travel prices and deals by hammering the search feature.

## User Story
**As a** platform security owner, **I want** have search/booking endpoints rate-limited against a competitor scraping Adren's re-exposed supplier pricing, **so that** RULES.md §5.4's distinct inbound-scraping concern (separate from per-supplier outbound rate limiting) is closed before GA.

## Acceptance Criteria
- Given a single client issues an abnormally high volume of search requests in a short window, when the inbound rate limiter evaluates, then further requests are throttled/rejected, distinct from and independent of the per-supplier outbound limiters in `supplier`.

## Developer Notes
- **PRD reference(s):** §5.4 OWASP-relevant concerns (RULES.md, inbound rate limiting)
- **Module(s)/Screen(s):** booking
- **Story points:** 5 — Inbound rate-limiting layer, deliberately distinct from the outbound per-supplier limiters already built in Phase 1.
- **Dependencies:** FND-13
- **Testing tier(s):** unit, integration (Testcontainers)

## Sub-tasks
- [NEW] Backend: inbound rate limiter on search/booking endpoints
- [NEW] Backend: unit test
- [NEW] Backend: integrationTest — burst-request scenario throttled
