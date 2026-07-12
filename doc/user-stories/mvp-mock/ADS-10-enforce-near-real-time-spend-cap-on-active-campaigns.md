---
id: ADS-10
epic: Ads/Campaign Management
phase: mock
status: not-started
story_points: 8
dependencies: ["ADS-07"]
labels: ["backend", "ads", "phase1"]
prd_references: ["§14.3", "§24.6"]
modules_or_screens: ["ads"]
testing_tiers: ["unit", "module (@ApplicationModuleTest)"]
---

# ADS-10: Enforce near-real-time spend-cap on active campaigns

## Summary (business)
The system continuously tracks how much a campaign has spent and automatically pauses it as soon as it nears its set budget limit, so spending essentially never goes meaningfully over the agreed amount. This protects both Adren and Consultants from unexpected or runaway advertising costs.

## User Story
**As a** Super Admin/Consultant, **I want** have a campaign's spend never meaningfully overshoot its budget cap, **so that** PRD §14.3's guardrail and §24.6's near-real-time NFR are both met.

## Acceptance Criteria
- Given a Live campaign's `spend_to_date` approaches `budget_cap`, when spend tracking updates, then the campaign transitions to SpendCapReached before processing lag allows a meaningful overshoot.

## Developer Notes
- **PRD reference(s):** §14.3 Controls & Guardrails; §24.6 NFR Ads/Campaign
- **Module(s)/Screen(s):** ads
- **Story points:** 8 — Near-real-time enforcement against a mocked spend feed in MVP is still non-trivial polling/threshold logic — the highest-uncertainty Ads story alongside ADS-01.
- **Dependencies:** ADS-07
- **Testing tier(s):** unit, module (@ApplicationModuleTest)

## Sub-tasks
- [NEW] Backend: spend-tracking poller (mocked feed in MVP) + threshold-triggered `SpendCapReached` transition
- [NEW] Backend: unit test — threshold boundary
- [NEW] Backend: module test — transition triggers correctly under simulated spend growth
