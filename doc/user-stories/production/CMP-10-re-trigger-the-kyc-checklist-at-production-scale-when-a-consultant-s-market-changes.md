---
id: CMP-10
epic: Compliance Execution
phase: production
status: not-started
story_points: 5
dependencies: ["FND-04", "FND-17"]
labels: ["backend", "compliance", "phase2"]
prd_references: ["§23.6", "§25"]
modules_or_screens: ["compliance"]
testing_tiers: ["integration (Testcontainers)"]
---

# CMP-10: Re-trigger the KYC checklist at production scale when a Consultant's market changes

## Summary (business)
When a travel consultant moves to a different country, the compliance checks and paperwork required of them (KYC, or "Know Your Customer" verification) need to switch to match the rules of their new location. This story makes sure that switch happens reliably even under heavy real-world usage and when multiple changes happen at once, so consultants are never left operating under the wrong country's rules due to a system glitch at scale.

## User Story
**As a** Super Admin, **I want** have a Consultant's KYC checklist re-trigger correctly for their new market when they relocate, even under real production data volume, **so that** PRD §23.6 Edge Case #13 and T18 hold under production scale and concurrent-edit conditions the MVP's FND-04 rule table wasn't stress-tested against.

## Acceptance Criteria
- Given a Consultant's declared home market changes after onboarding (e.g. relocates India → UK) under production load, when the change is saved, then the system re-triggers the KYC checklist for the new market rather than leaving the account under the original market's rules, verified under concurrent-access conditions.

## Developer Notes
- **PRD reference(s):** §23.6 Edge Case #13; §25 T18
- **Module(s)/Screen(s):** compliance
- **Story points:** 5 — Production-scale/concurrency hardening of a rule that FND-04's MVP rule table already implements at small scale.
- **Dependencies:** FND-04, FND-17
- **Testing tier(s):** integration (Testcontainers)

## Sub-tasks
- [EXTEND] Backend: market-change KYC re-trigger hardened for concurrent-edit safety
- [NEW] Backend: integrationTest — concurrent market-change scenario
