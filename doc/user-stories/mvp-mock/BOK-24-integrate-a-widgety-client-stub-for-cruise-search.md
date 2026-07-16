---
id: BOK-24
epic: Booking Core
phase: mock
status: not-started
story_points: 3
dependencies: []
labels: ["backend", "booking", "supplier", "phase1"]
prd_references: ["§10.1", "§10.2.6"]
modules_or_screens: ["supplier"]
testing_tiers: ["unit"]
---

# BOK-24: Integrate a Widgety client stub for cruise search

## Summary (business)
Widgety supplies cruise inventory. This story gives the platform a working (stubbed, sandbox-ready) connection to Widgety so cruise line items (BOK-06) have a real supplier client to call, and establishes how Widgety's own multi-port itinerary structure gets flattened into Adren's single-line-item model.

## User Story
**As a** backend engineer, **I want** a `WidgetyClient` component that returns normalized `SupplierSearchResult`s from a stubbed search call, following the established `HotelbedsClient` pattern, **so that** BOK-06's cruise line items can be sourced from a real (stubbed) supplier client, per PRD §10.1/§10.2.6.

## Acceptance Criteria
- Given a cruise search is issued, when `WidgetyClient.search(...)` is called, then it returns a `List<SupplierSearchResult>` tagged `SupplierId.WIDGETY`, following the normalized shape used by the other clients.
- Given Widgety's multi-port `Itinerary` structure (PRD §10.2.6) doesn't map 1:1 onto a single line item, when the stub models its response, then port-by-port detail is represented as line-item metadata (not separate line items), matching the flattening rule the PRD specifies.
- Given "cabin category sold out" is a distinct, common failure mode for cruise (vs. the "rate expired" pattern common to hotels), when the client's error contract is defined, then it's modeled as its own exception/case rather than reusing a generic "unavailable" outcome.

## Developer Notes
- **PRD reference(s):** §10.1 Supplier Overview; §10.2.6 Widgety (Cruise)
- **Module(s)/Screen(s):** supplier
- **Story points:** 3 — Plain REST API key auth, but the multi-port-to-single-line-item flattening rule adds a small amount of shape-design work beyond a pure pass-through stub.
- **Dependencies:** none — self-contained stub.
- **Testing tier(s):** unit

## Sub-tasks
- [NEW] Backend: `WidgetyClient` in `com.adren.travel.supplier.internal.widgety`, `search(...)` returning stubbed `SupplierSearchResult` tagged `SupplierId.WIDGETY`
- [NEW] Backend: port-by-port detail carried as line-item metadata, not separate results, per §10.2.6's flattening rule
- [NEW] Backend: `WidgetyCabinSoldOutException` distinct from the plain no-availability case
- [NEW] Backend: unit test — result shape correct, port metadata flattening exercised
