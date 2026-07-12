---
id: HRD-03
epic: Hardening
phase: mock
status: not-started
story_points: 5
dependencies: ["HRD-01", "HRD-02"]
labels: ["backend", "notification", "phase1"]
prd_references: ["§2.2"]
modules_or_screens: ["notification"]
testing_tiers: ["unit", "integration (Testcontainers)"]
---

# HRD-03: Make every notification listener idempotent

## Summary (business)
Behind-the-scenes system hiccups can occasionally cause the same alert to try to send twice. This story guarantees a traveler or consultant never receives a duplicate notification even if the system retries, protecting the customer experience from confusing repeat messages.

## User Story
**As a** platform reliability owner, **I want** have every notification listener safe to run twice for the same event, **so that** RULES.md §2.2's mandatory idempotency rule is met — a traveler must never be double-notified on an at-least-once redelivery.

## Acceptance Criteria
- Given a notification listener is redelivered the same event after a crash-and-retry, when it runs a second time, then no duplicate notification is sent — a dedup key (event_id, listener_name) with a DB unique constraint prevents the resend.

## Developer Notes
- **PRD reference(s):** §2.2 Idempotency is mandatory (RULES.md)
- **Module(s)/Screen(s):** notification
- **Story points:** 5 — Explicit reconciliation item flagged in RULES.md §2.2 as required before the notification listener's real body ships — sequenced right after HRD-01/02.
- **Dependencies:** HRD-01, HRD-02
- **Testing tier(s):** unit, integration (Testcontainers)

## Sub-tasks
- [NEW] Backend: `processed_events` table with unique `(event_id, listener_name)` constraint
- [EXTEND] Backend: every listener checks-then-inserts before dispatching
- [NEW] Backend: unit test — duplicate delivery is a no-op
- [NEW] Backend: integrationTest — simulated crash-and-retry redelivery
