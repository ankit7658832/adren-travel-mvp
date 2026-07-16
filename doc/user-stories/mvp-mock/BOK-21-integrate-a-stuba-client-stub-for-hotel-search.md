---
id: BOK-21
epic: Booking Core
phase: mock
status: not-started
story_points: 3
dependencies: []
labels: ["backend", "booking", "supplier", "phase1"]
prd_references: ["§10.1", "§10.2.2"]
modules_or_screens: ["supplier"]
testing_tiers: ["unit"]
---

# BOK-21: Integrate a STUBA client stub for hotel search

## Summary (business)
STUBA is the platform's second hotel source (after Hotelbeds), giving the business a wider range of hotel inventory and a fallback when Hotelbeds doesn't have a property or rate. This story gives the platform a working (stubbed, sandbox-ready) connection to STUBA following the same shape as the existing Hotelbeds connection, so hotel search results can eventually be aggregated across both suppliers.

## User Story
**As a** backend engineer, **I want** a `StubaClient` component that returns normalized `SupplierSearchResult`s from a stubbed search call, matching `HotelbedsClient`'s existing pattern, **so that** STUBA can be aggregated into hotel search results and BOK-20's dedup logic has a second real hotel supplier to reconcile against, per PRD §10.1/§10.2.2.

## Acceptance Criteria
- Given a hotel search is issued, when `StubaClient.search(locationCode, checkIn, checkOut)` is called, then it returns a `List<SupplierSearchResult>` tagged `SupplierId.STUBA`, following the same normalized shape `HotelbedsClient` returns.
- Given STUBA's session-token auth model (PRD §10.2.2), when the client is invoked, then session acquisition/refresh is isolated in its own method so the real XML-API call can later replace only that method without touching the normalized-result contract.
- Given a stale/expired STUBA session, when `search` is called, then the client is structured to retry once after re-authentication before surfacing a failure (per PRD §10.2.2's error-handling rule) — the stub documents this via a `TODO` at the real-call site, matching `HotelbedsClient`'s existing stub convention.

## Developer Notes
- **PRD reference(s):** §10.1 Supplier Overview; §10.2.2 STUBA (Hotels)
- **Module(s)/Screen(s):** supplier
- **Story points:** 3 — Mirrors `HotelbedsClient`'s existing stub shape (`backend/src/main/java/com/adren/travel/supplier/internal/hotelbeds/HotelbedsClient.java`); smaller than Hotelbeds' own scope since the normalized-result contract and rate-limit/circuit-breaker concerns are handled generically (BOK-26), not per-client.
- **Dependencies:** none — self-contained stub, same footing as the pre-existing `HotelbedsClient` scaffold.
- **Testing tier(s):** unit

## Sub-tasks
- [NEW] Backend: `StubaClient` in `com.adren.travel.supplier.internal.stuba`, `search(...)` returning stubbed `SupplierSearchResult` tagged `SupplierId.STUBA`
- [NEW] Backend: session-token acquisition/refresh isolated as its own method (stub, `TODO` for real XML-API call per §10.2.2)
- [NEW] Backend: unit test — result shape matches `SupplierSearchResult`, `SupplierId.STUBA` tag correct
