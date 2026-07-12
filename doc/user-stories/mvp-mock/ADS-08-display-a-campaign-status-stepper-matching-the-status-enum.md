---
id: ADS-08
epic: Ads/Campaign Management
phase: mock
status: not-started
story_points: 3
dependencies: ["ADS-07"]
labels: ["frontend", "ads", "phase1"]
prd_references: ["§21.8", "§20.13"]
modules_or_screens: ["Campaign Builder (21.8)"]
testing_tiers: ["component test"]
---

# ADS-08: Display a campaign status stepper matching the status enum

## Summary (business)
Consultants see a simple visual progress tracker showing exactly where their campaign stands - awaiting approval, under review, live, or rejected - and it always matches what's actually happening behind the scenes. This gives Consultants clear, trustworthy visibility into their campaign's progress without confusion.

## User Story
**As a** Consultant, **I want** see a visual status stepper (Pending Approval → Pending Policy Review → Live / Rejected) for my campaign, **so that** PRD §21.8's status-tracking requirement matches §20.13's enum exactly.

## Acceptance Criteria
- Given a campaign is in any of the six `status` enum values, when the Consultant views the Campaign Builder, then the stepper highlights the exact matching stage — no divergence between UI labels and the backend enum.

## Developer Notes
- **PRD reference(s):** §21.8 Campaign Builder (status tracking); §20.13 status enum
- **Module(s)/Screen(s):** Campaign Builder (21.8)
- **Story points:** 3 — Presentational component driven entirely by ADS-02's enum — no new backend work.
- **Dependencies:** ADS-07
- **Testing tier(s):** component test

## Sub-tasks
- [NEW] Frontend: `CampaignStatusStepper` presentational component
- [NEW] Frontend: component test — one case per enum value
