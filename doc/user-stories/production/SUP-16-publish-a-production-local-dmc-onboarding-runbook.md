---
id: SUP-16
epic: Supplier Live Integrations
phase: production
status: not-started
story_points: 3
dependencies: ["DMC-02"]
labels: ["ops", "supplier", "phase2"]
prd_references: ["§10.3"]
modules_or_screens: ["supplier"]
testing_tiers: ["integration (Testcontainers)"]
---

# SUP-16: Publish a production Local DMC onboarding runbook

## Summary (business)
This creates a written playbook with clear service-level timelines and verification steps for approving new local destination management companies (regional partners who provide on-the-ground travel services) once we're reviewing real applicants instead of test cases. It ensures onboarding decisions are consistent and reliable regardless of which staff member handles the review.

## User Story
**As a** Super Admin, **I want** have a documented review SLA and verification-step process for Local DMC onboarding at production scale, **so that** PRD §10.3's onboarding workflow (DMC-02) has an operational runbook once real Local DMCs, not MVP test fixtures, are being reviewed.

## Acceptance Criteria
- Given a real Local DMC submission is received in production, when the runbook is followed, then the documented review SLA and verification-step sequence is applied consistently, not improvised per reviewer.

## Developer Notes
- **PRD reference(s):** §10.3 Local DMC Onboarding & Vetting Workflow
- **Module(s)/Screen(s):** supplier
- **Story points:** 3 — Documentation/process deliverable, not code — but a real production-readiness gap the MVP's DMC-02 story didn't need to address.
- **Dependencies:** DMC-02
- **Testing tier(s):** integration (Testcontainers)

## Sub-tasks
- [NEW] Infra: Local DMC onboarding runbook — review SLA and verification-step checklist
- [NEW] Infra: README / RULES.md documentation update
- [NEW] Infra: verification step (local run and/or CI check)
