---
id: BOK-27
epic: Booking Core
phase: mock
status: not-started
story_points: 5
dependencies: ["BOK-21", "BOK-22", "BOK-24", "BOK-25"]
labels: ["backend", "booking", "supplier", "phase1"]
prd_references: ["§10.5", "§10.2.1", "§10.2.6", "§10.2.7"]
modules_or_screens: ["supplier"]
testing_tiers: ["unit", "integration (Testcontainers)"]
---

# BOK-27: Sync and cache static supplier content

## Summary (business)
Search results currently show no real hotel rating, property photos, ship/cabin images, or activity descriptions, because that "static" content (as opposed to live price/availability) has never been pulled from any supplier — every client stub returns `null` for rating today. This story builds the scheduled job and cache that pulls this descriptive content from each supplier on its own cadence and serves it alongside live search results, so listings look complete instead of bare.

## User Story
**As a** backend engineer, **I want** a scheduled content-sync service that pulls and caches each supplier's static content (property/ship/activity name, rating, photos, descriptions) on a per-supplier cadence, **so that** search results can be enriched with real descriptive content instead of `null` fields, per PRD §10.5, and BOK-20's dedup logic has real property attributes (not just supplier IDs) to match on.

## Acceptance Criteria
- Given a supplier's content-sync job runs, when it completes, then the fetched static content (name, rating, photos, description as applicable) is written to a local cache table keyed by `(SupplierId, supplierContentId)`, not fetched synchronously on every search.
- Given hotel suppliers (Hotelbeds, STUBA, TBO) sync nightly, cruise (Widgety) syncs weekly, and activities (HBActivities) sync nightly (per §10.2.1/§10.2.6/§10.2.7's differing cadences), when the scheduler is configured, then each supplier's job runs on its own PRD-specified cadence, not one shared interval.
- Given a search result is aggregated, when the normalized `SupplierSearchResult` is built, then it's enriched from the content cache (e.g., `star_rating`) instead of hard-coding `null`, closing the gap the existing `HotelbedsClient` stub comment calls out ("real rating requires supplier content sync — not wired for any supplier yet").
- Given a sync job fails or a supplier's cached content is older than a defined staleness threshold, when the failure/staleness occurs, then it's surfaced in a form HRD-13 can alert on (this story provides the underlying signal; HRD-13 builds the Super Admin-facing alert).

## Developer Notes
- **PRD reference(s):** §10.5 Inventory Sync; §10.2.1 Hotelbeds (nightly Content API sync); §10.2.6 Widgety (weekly ship/deck-plan sync); §10.2.7 HBActivities (nightly description/image sync)
- **Module(s)/Screen(s):** supplier
- **Story points:** 5 — One generic scheduled-sync mechanism configured per-supplier (cadence, content shape), plus a cache table and the search-time enrichment read path; not six bespoke sync jobs.
- **Dependencies:** BOK-21, BOK-22, BOK-24, BOK-25 (the clients whose content this syncs; Transferz and Mystifly are excluded per §10.2.4/§10.2.5 — transfers and flights have no meaningful static-content analogue)
- **Testing tier(s):** unit, integration (Testcontainers)

## Sub-tasks
- [NEW] Backend: `SupplierContentCache` entity/table (`SupplierId`, `supplierContentId`, name, rating, photos/description, `lastSyncedAt`) + Flyway migration
- [NEW] Backend: `SupplierContentSyncService` with a per-supplier `@Scheduled` job (nightly for hotels/activities, weekly for cruise, per §10.5's differing cadences)
- [EXTEND] Backend: `SupplierAggregationService` enriches normalized results from the content cache instead of returning `null` rating
- [NEW] Backend: staleness signal (cached content older than threshold) exposed for HRD-13 to alert on
- [NEW] Backend: unit test — per-supplier cadence configuration honored, enrichment applied correctly
- [NEW] Backend: integrationTest — scheduled sync writes to cache table (Testcontainers Postgres)
