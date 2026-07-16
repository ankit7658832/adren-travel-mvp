---
id: BOK-25
epic: Booking Core
phase: mock
status: not-started
story_points: 2
dependencies: []
labels: ["backend", "booking", "supplier", "phase1"]
prd_references: ["§10.1", "§10.2.7"]
modules_or_screens: ["supplier"]
testing_tiers: ["unit"]
---

# BOK-25: Integrate an HBActivities client stub for activity search

## Summary (business)
HBActivities supplies tours/experiences inventory. This story gives the platform a working (stubbed, sandbox-ready) connection to HBActivities so activity line items (BOK-07) have a real supplier client to call, including the time-slot-based availability model that distinguishes activities from date-range products like hotels.

## User Story
**As a** backend engineer, **I want** an `HbActivitiesClient` component that returns normalized `SupplierSearchResult`s from a stubbed search call, following the established `HotelbedsClient` pattern, **so that** BOK-07's activity line items can be sourced from a real (stubbed) supplier client, per PRD §10.1/§10.2.7.

## Acceptance Criteria
- Given an activity search is issued, when `HbActivitiesClient.search(...)` is called, then it returns a `List<SupplierSearchResult>` tagged `SupplierId.HBACTIVITIES`, following the normalized shape used by the other clients.
- Given HBActivities availability is time-slot based rather than date-range based (PRD §10.2.7), when the stub models its response, then available slots are represented explicitly (not collapsed into a single per-day availability flag) so slot-specific sellouts can later be distinguished from a fully-unavailable day.

## Developer Notes
- **PRD reference(s):** §10.1 Supplier Overview; §10.2.7 HBActivities (Activities)
- **Module(s)/Screen(s):** supplier
- **Story points:** 2 — Plain REST API key auth, same footing as Transferz; the only wrinkle is representing slot-based (not date-range) availability in the stub shape.
- **Dependencies:** none — self-contained stub.
- **Testing tier(s):** unit

## Sub-tasks
- [NEW] Backend: `HbActivitiesClient` in `com.adren.travel.supplier.internal.hbactivities`, `search(...)` returning stubbed `SupplierSearchResult` tagged `SupplierId.HBACTIVITIES`
- [NEW] Backend: available time slots represented explicitly in the stub result shape, per §10.2.7
- [NEW] Backend: unit test — result shape correct, slot representation exercised
