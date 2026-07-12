---
id: HRD-05
epic: Hardening
phase: mock
status: not-started
story_points: 8
dependencies: ["FIN-16", "HRD-03"]
labels: ["backend", "booking", "payments", "notification", "phase1"]
prd_references: ["§12.5"]
modules_or_screens: ["booking", "payments", "notification"]
testing_tiers: ["unit", "module (@ApplicationModuleTest)", "integration (Testcontainers)"]
---

# HRD-05: Implement the full cancellation workflow across policy check, approval, and refund

## Summary (business)
This story connects all the separate steps of cancelling a booking (checking the cancellation rules, calculating any refund or penalty, getting approval if needed, and issuing the refund) into one smooth, automatic process, so cancellations are handled consistently and customers are kept informed throughout.

## User Story
**As a** Consultant, **I want** have a cancellation move through policy check → refund/penalty calculation → my approval (if needed) → refund processed as one coherent workflow, **so that** PRD §12.5's workflow is realized end-to-end, connecting FIN-16's calculation to a real notification and status trail.

## Acceptance Criteria
- Given a cancellation with no penalty is submitted, when the workflow runs, then it completes without requiring explicit approval, and both email and configured secondary-channel notifications fire on refund per T11-adjacent §22.7 rule.

## Developer Notes
- **PRD reference(s):** §12.5 Cancellation & Dispute Handling
- **Module(s)/Screen(s):** booking, payments, notification
- **Story points:** 8 — End-to-end orchestration story tying together FIN-16 (calculation), booking's cancellation entry point, and HRD-01–03's notification dispatch.
- **Dependencies:** FIN-16, HRD-03
- **Testing tier(s):** unit, module (@ApplicationModuleTest), integration (Testcontainers)

## Sub-tasks
- [EXTEND] Backend: cancellation endpoint orchestrates FIN-16's policy-check/approval/refund state machine end-to-end
- [NEW] Backend: notification dispatch on refund completion (both channels per §22.7)
- [NEW] Backend: unit test
- [NEW] Backend: integrationTest — full cancellation-to-refund-to-notification path
