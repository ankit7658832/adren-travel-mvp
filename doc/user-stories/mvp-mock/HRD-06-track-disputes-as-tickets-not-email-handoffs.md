---
id: HRD-06
epic: Hardening
phase: mock
status: not-started
story_points: 5
dependencies: ["FIN-16"]
labels: ["backend", "frontend", "notification", "booking", "phase1"]
prd_references: ["§12.5"]
modules_or_screens: ["notification", "booking"]
testing_tiers: ["unit", "module (@ApplicationModuleTest)", "component test"]
---

# HRD-06: Track disputes as tickets, not email handoffs

## Summary (business)
When a customer disputes a charge or booking issue, it will now be logged as a trackable case with a clear status, instead of just being handled through back-and-forth emails that are easy to lose track of. This gives both consultants and administrators visibility into open issues until they're resolved.

## User Story
**As a** Consultant/Super Admin, **I want** have a flagged dispute create a trackable ticket with status, not just an email, **so that** PRD §12.5's dispute-tracking requirement is met.

## Acceptance Criteria
- Given a dispute is flagged on a booking, when the flag is submitted, then a `DisputeTicket` (FIN-16) is visible with a status the Consultant and Super Admin can both track to resolution, not just an emailed notice.

## Developer Notes
- **PRD reference(s):** §12.5 Cancellation & Dispute Handling (dispute flagging)
- **Module(s)/Screen(s):** notification, booking
- **Story points:** 5 — UI layer over FIN-16's `DisputeTicket` entity, with status tracking visible to both roles.
- **Dependencies:** FIN-16
- **Testing tier(s):** unit, module (@ApplicationModuleTest), component test

## Sub-tasks
- [NEW] Backend: `GET /api/v1/disputes?consultantId=` paginated endpoint
- [NEW] Frontend: `useDisputeTickets` hook (React Query for server data per RULES.md §7.1)
- [NEW] Frontend: `DisputeTicketTracker.tsx` component — all 5 PRD Part 21 states (default/loading/success/empty/error)
- [NEW] Frontend: component test (Testing Library, co-located, asserts on role/label per RULES.md §7.3)
