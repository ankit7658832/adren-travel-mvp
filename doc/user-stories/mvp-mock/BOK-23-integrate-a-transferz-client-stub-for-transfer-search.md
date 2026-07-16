---
id: BOK-23
epic: Booking Core
phase: mock
status: not-started
story_points: 2
dependencies: []
labels: ["backend", "booking", "supplier", "phase1"]
prd_references: ["§10.1", "§10.2.5"]
modules_or_screens: ["supplier"]
testing_tiers: ["unit"]
---

# BOK-23: Integrate a Transferz client stub for transfer search

## Summary (business)
Transferz supplies ground-transfer inventory across 150+ countries. This story gives the platform a working (stubbed, sandbox-ready) connection to Transferz so transfer line items (BOK-05) have a real supplier client to call instead of hand-built fixture data.

## User Story
**As a** backend engineer, **I want** a `TransferzClient` component that returns normalized `SupplierSearchResult`s from a stubbed search call, following the established `HotelbedsClient` pattern, **so that** BOK-05's transfer line items can be sourced from a real (stubbed) supplier client, per PRD §10.1/§10.2.5.

## Acceptance Criteria
- Given a transfer search is issued, when `TransferzClient.search(pickupPoint, dropoffPoint, date)` is called, then it returns a `List<SupplierSearchResult>` tagged `SupplierId.TRANSFERZ`, following the normalized shape used by the hotel clients.
- Given Transferz distinguishes "no coverage at this location" from "no availability" (PRD §10.2.5), when the stub models its response, then the return type/exception shape leaves room for that distinction (e.g., a checked `TransferzNoCoverageException` alongside the plain empty-result case) rather than collapsing both into one "no results" outcome.

## Developer Notes
- **PRD reference(s):** §10.1 Supplier Overview; §10.2.5 Transferz (Transfers)
- **Module(s)/Screen(s):** supplier
- **Story points:** 2 — Simplest of the new clients: plain REST API key auth, no session/token lifecycle to model at stub stage.
- **Dependencies:** none — self-contained stub.
- **Testing tier(s):** unit

## Sub-tasks
- [NEW] Backend: `TransferzClient` in `com.adren.travel.supplier.internal.transferz`, `search(...)` returning stubbed `SupplierSearchResult` tagged `SupplierId.TRANSFERZ`
- [NEW] Backend: `TransferzNoCoverageException` distinct from the plain no-availability (empty list) case, per §10.2.5
- [NEW] Backend: unit test — result shape correct, no-coverage vs. no-availability distinction exercised
