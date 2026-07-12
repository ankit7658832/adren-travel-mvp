---
id: BOK-16
epic: Booking Core
phase: mock
status: not-started
story_points: 5
dependencies: ["BOK-13"]
labels: ["backend", "booking", "phase1"]
prd_references: ["§23.1", "§22", "§25"]
modules_or_screens: ["booking"]
testing_tiers: ["unit", "integration (Testcontainers)"]
---

# BOK-16: Prevent double-booking of the last available inventory unit under concurrent requests

## Summary (business)
If two customers try to book the very last available seat, room, or slot at the same moment, the system ensures only one booking succeeds and the other person is told it's no longer available, rather than accidentally selling the same inventory twice. This protects the business from overbooking situations that would otherwise require awkward, costly fixes and damage customer trust.

## User Story
**As a** platform reliability owner, **I want** have the second of two simultaneous booking attempts on the last unit fail gracefully, **so that** PRD §23.1 Edge Case #1 is closed with a real concurrency guarantee, not just documented as a risk.

## Acceptance Criteria
- Given two Users under the same Consultant attempt to book the last available inventory unit simultaneously, when the second commit is attempted, then it fails with an `OptimisticLockException` mapped to a 'no longer available' message, not a duplicate booking (T21).

## Developer Notes
- **PRD reference(s):** §23.1 Edge Case #1; §22.x concurrency; §25 T21
- **Module(s)/Screen(s):** booking
- **Story points:** 5 — Concrete, well-specified concurrency fix (`@Version` + service-layer exception mapping) per backend-best-practices §3.
- **Dependencies:** BOK-13
- **Testing tier(s):** unit, integration (Testcontainers)

## Sub-tasks
- [NEW] Backend: `@Version` field on `Itinerary`/`Booking`
- [NEW] Backend: service-layer mapping of `OptimisticLockException` → domain 'no longer available' exception
- [NEW] Backend: unit test
- [NEW] Backend: integrationTest — two concurrent commits against Testcontainers Postgres, second fails
