---
id: BOK-22
epic: Booking Core
phase: mock
status: not-started
story_points: 3
dependencies: []
labels: ["backend", "booking", "supplier", "phase1"]
prd_references: ["§10.1", "§10.2.3"]
modules_or_screens: ["supplier"]
testing_tiers: ["unit"]
---

# BOK-22: Integrate a TBO client stub for hotel search

## Summary (business)
TBO is the platform's third hotel source, particularly strong for Indian-market inventory. This story gives the platform a working (stubbed, sandbox-ready) connection to TBO following the same shape as the existing Hotelbeds/STUBA connections, so hotel search can draw from a supplier with strong domestic coverage in one of the platform's key markets.

## User Story
**As a** backend engineer, **I want** a `TboClient` component that returns normalized `SupplierSearchResult`s from a stubbed search call, matching the `HotelbedsClient`/`StubaClient` pattern, **so that** TBO can be aggregated into hotel search results and BOK-20's dedup logic has a third hotel supplier to reconcile against, per PRD §10.1/§10.2.3.

## Acceptance Criteria
- Given a hotel search is issued, when `TboClient.search(locationCode, checkIn, checkOut)` is called, then it returns a `List<SupplierSearchResult>` tagged `SupplierId.TBO`, following the same normalized shape as `HotelbedsClient`/`StubaClient`.
- Given TBO's session-scoped `TraceId` requirement (PRD §10.2.3 — all calls within a search session must reuse the same `TraceId`), when the client is invoked, then the `TraceId` is threaded as an explicit parameter/return value rather than hidden client-internal state, so the caller (the in-progress itinerary draft) can persist and reuse it across the search session.
- Given a stale/expired TBO `TraceId`, when `search` is called with it, then the client is structured to signal "full re-search required" rather than attempting a partial retry (per PRD §10.2.3) — documented via a `TODO` at the real-call site, matching the existing stub convention.

## Developer Notes
- **PRD reference(s):** §10.1 Supplier Overview; §10.2.3 TBO (Hotels)
- **Module(s)/Screen(s):** supplier
- **Story points:** 3 — Mirrors `HotelbedsClient`/`StubaClient`'s stub shape; the one TBO-specific wrinkle (`TraceId` session threading) is a signature/contract concern, not implementation complexity, at stub stage.
- **Dependencies:** none — self-contained stub, same footing as `HotelbedsClient`/`BOK-21`.
- **Testing tier(s):** unit

## Sub-tasks
- [NEW] Backend: `TboClient` in `com.adren.travel.supplier.internal.tbo`, `search(...)` returning stubbed `SupplierSearchResult` tagged `SupplierId.TBO`
- [NEW] Backend: `TraceId` threaded explicitly through the search signature (stub value), `TODO` for real TBO API call per §10.2.3
- [NEW] Backend: unit test — result shape matches `SupplierSearchResult`, `SupplierId.TBO` tag correct, `TraceId` round-trips
