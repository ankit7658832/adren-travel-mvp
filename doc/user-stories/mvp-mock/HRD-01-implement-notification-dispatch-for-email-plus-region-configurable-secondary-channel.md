---
id: HRD-01
epic: Hardening
phase: mock
status: not-started
story_points: 8
dependencies: ["FND-21", "BOK-02"]
labels: ["backend", "notification", "phase1"]
prd_references: ["§15", "§22.7"]
modules_or_screens: ["notification"]
testing_tiers: ["unit", "module (@ApplicationModuleTest)", "integration (Testcontainers)"]
---

# HRD-01: Implement notification dispatch for email plus region-configurable secondary channel

## Summary (business)
Travelers and consultants will get booking updates by email everywhere, plus a second channel that fits their region automatically (WhatsApp in India/Dubai, text message in the UK/US/Australia/Denmark). This replaces a placeholder that currently sends nothing, so people actually find out when something happens with their booking.

## User Story
**As a** User/Consultant, **I want** receive notifications by email everywhere and by a region-appropriate secondary channel (WhatsApp for India/Dubai, SMS for UK/US/Australia/Denmark), **so that** PRD §15's channel model and §22.7's T11 requirement are implemented, replacing today's empty `BookingNotificationListener` TODO stub.

## Acceptance Criteria
- Given a booking is confirmed for a Dubai-based Consultant, when the confirmation notification fires, then WhatsApp is used as the default secondary channel unless the Consultant has overridden this preference (T11).
- Given a booking is confirmed for a UK-based Consultant, when the confirmation notification fires, then SMS is used as the region default secondary channel.

## Developer Notes
- **PRD reference(s):** §15 Notifications & Cancellation Management; §22.7 T11
- **Module(s)/Screen(s):** notification
- **Story points:** 8 — Fills in the currently-empty reference listener with real email + region-routed secondary-channel dispatch across two provider integrations.
- **Dependencies:** FND-21, BOK-02
- **Testing tier(s):** unit, module (@ApplicationModuleTest), integration (Testcontainers)

## Sub-tasks
- [EXTEND] Backend: `BookingNotificationListener.on(BookingConfirmedEvent)` real implementation (email + region-routed secondary channel)
- [NEW] Backend: WhatsApp provider client
- [NEW] Backend: SMS provider client
- [NEW] Backend: unit test — region→channel routing
- [NEW] Backend: integrationTest — full dispatch against LocalStack-emulated provider endpoints where applicable
