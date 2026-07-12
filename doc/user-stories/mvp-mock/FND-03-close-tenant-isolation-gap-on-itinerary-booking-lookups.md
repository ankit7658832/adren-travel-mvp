---
id: FND-03
epic: Foundation
phase: mock
status: not-started
story_points: 5
dependencies: ["FND-01", "FND-02"]
labels: ["backend", "foundation", "security", "phase1"]
prd_references: ["§5.2", "§22.6"]
modules_or_screens: ["booking"]
testing_tiers: ["unit", "module (@ApplicationModuleTest)"]
---

# FND-03: Close tenant-isolation gap on itinerary/booking lookups

## Summary (business)
This story stops one travel consultant from being able to view or change another consultant's trip plans, even by guessing a web link. Without this protection, sensitive customer itineraries and bookings could leak between competing consultants, which would be a serious trust and privacy failure.

## User Story
**As a** Consultant, **I want** be certain another Consultant can never read or act on my itinerary even if they guess or observe its UUID, **so that** the platform is not exposed to Broken Object Level Authorization (OWASP API1:2023), which RULES.md §5.2 flags as the top realistic risk.

## Acceptance Criteria
- Given Consultant B calls `saveAsQuotation` with an itinerary_id belonging to Consultant A, when the authenticated principal's consultant_id does not match the itinerary's owner, then the request is rejected, not silently executed.
- Given SUPER_ADMIN calls the same endpoint, when the 'view/act on all' exception path is used, then it succeeds via its own explicitly `@PreAuthorize`'d path, not the default.

## Developer Notes
- **PRD reference(s):** §5.2 (RULES.md); §22.6 BYOS (scoping pattern)
- **Module(s)/Screen(s):** booking
- **Story points:** 5 — Security-critical fix scoped to existing BookingServiceImpl/ItineraryController call sites; well-bounded.
- **Dependencies:** FND-01, FND-02
- **Testing tier(s):** unit, module (@ApplicationModuleTest)

## Sub-tasks
- [EXTEND] Backend: `BookingServiceImpl` methods re-fetch by (itineraryId, authenticated consultantId), never client-supplied consultantId alone
- [NEW] Backend: SUPER_ADMIN 'view all' explicit code path
- [NEW] Backend: unit test — cross-tenant access attempt
- [NEW] Backend: module test — cross-tenant access attempt end-to-end through the Api
