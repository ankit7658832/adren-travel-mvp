---
id: HRD-02
epic: Hardening
phase: mock
status: not-started
story_points: 8
dependencies: ["HRD-01"]
labels: ["backend", "notification", "phase1"]
prd_references: ["§15"]
modules_or_screens: ["notification", "booking", "payments", "ai", "ads"]
testing_tiers: ["module (@ApplicationModuleTest)", "integration (Testcontainers)"]
---

# HRD-02: Wire all PRD §15 notification trigger events

## Summary (business)
Right now only booking confirmations trigger a notification. This story turns on alerts for all the moments that matter: payment received, cancellations, refunds, approvals needed, marketing campaign changes, and low account balance warnings, so nothing important goes unnoticed.

## User Story
**As a** User/Consultant, **I want** be notified on booking confirmed, payment received, cancellation, refund, AI approval needed, campaign status change, and credit threshold breach, **so that** PRD §15's full trigger-event list is wired, not just booking confirmation.

## Acceptance Criteria
- Given any of the seven PRD §15 trigger events occurs, when the corresponding domain event is published by its owning module, then the `notification` module's listeners consume it and dispatch per HRD-01's channel routing.

## Developer Notes
- **PRD reference(s):** §15 Notifications (Trigger events)
- **Module(s)/Screen(s):** notification, booking, payments, ai, ads
- **Story points:** 8 — Requires a listener per trigger event across four other modules' already-published (or newly-added) domain events — broad but mechanically repetitive.
- **Dependencies:** HRD-01
- **Testing tier(s):** module (@ApplicationModuleTest), integration (Testcontainers)

## Sub-tasks
- [NEW] Backend: `@ApplicationModuleListener` per trigger event (payment received, cancellation, refund, AI approval needed, campaign status, credit threshold)
- [NEW] Backend: any missing domain event added to its owning module (e.g. `CreditThresholdBreachedEvent` in `payments`)
- [NEW] Backend: module test per trigger
- [NEW] Backend: integrationTest — at least one full cross-module trigger path
