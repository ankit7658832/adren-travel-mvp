---
id: FND-23
epic: Foundation
phase: mock
status: not-started
story_points: 3
dependencies: []
labels: ["backend", "foundation", "phase1"]
prd_references: ["§3.4"]
modules_or_screens: ["booking"]
testing_tiers: ["unit", "module (@ApplicationModuleTest)"]
---

# FND-23: Convert all collection endpoints to paginated responses

## Summary (business)
This story ensures that any screen listing multiple records (such as a consultant's bookings) loads them in manageable pages rather than trying to load an unlimited list all at once. This prevents the platform from slowing down or crashing for consultants who accumulate thousands of bookings over time.

## User Story
**As a** API consumer, **I want** receive a paginated response from any endpoint returning a collection, never a bare unbounded list, **so that** a Consultant who accumulates thousands of bookings never causes an unbounded response, per RULES.md §3.4 — `BookingApi.findBookingsByConsultant` currently returns a bare `List<UUID>` and must not be wired to a controller as-is.

## Acceptance Criteria
- Given `GET /api/v1/bookings?consultantId=...` is called, when the response is built, then it returns `{content, page, size, totalElements, totalPages}`, never a bare array.
- Given `findBookingsByConsultant` is called from any caller, when the signature is inspected, then it takes and returns `Page<...>`, not `List<UUID>`.

## Developer Notes
- **PRD reference(s):** §3.4 Pagination (RULES.md, reconciliation item #8)
- **Module(s)/Screen(s):** booking
- **Story points:** 3 — Scoped, mechanical fix (`Pageable` end-to-end) but blocks BOK-08 and HRD-07 from being wired to controllers incorrectly.
- **Dependencies:** None
- **Testing tier(s):** unit, module (@ApplicationModuleTest)

## Sub-tasks
- [EXTEND] Backend: `BookingApi.findBookingsByConsultant` → `Page<UUID>`/DTO page
- [NEW] Backend: repository query takes `Pageable` end-to-end (no in-memory skip/limit)
- [NEW] Backend: unit test — page metadata correctness
- [NEW] Backend: module test
