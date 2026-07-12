---
id: MADS-05
epic: Meta Ads API Real Integration
phase: production
status: not-started
story_points: 8
dependencies: ["MADS-04", "ADS-10"]
labels: ["backend", "ads", "phase2"]
prd_references: ["§14.3", "§24.6"]
modules_or_screens: ["ads"]
testing_tiers: ["unit", "integration (Testcontainers)"]
---

# MADS-05: Enforce spend caps via real Meta API-level budget controls

## Summary (business)
This story adds a safety net so that ad campaigns cannot spend more than their approved budget, by using Facebook/Instagram's own built-in spending limits in addition to Adren's own monitoring. This protects consultants and Adren from unexpected overspending, especially if our own tracking is briefly delayed.

## User Story
**As a** Super Admin/Consultant, **I want** have a campaign's spend cap enforced by Meta's own budget controls, reconciled against Adren's near-real-time tracking, **so that** ADS-10's MVP-mocked spend-tracking is reconciled against Meta's authoritative real spend data, closing the gap processing lag could otherwise introduce.

## Acceptance Criteria
- Given a Live campaign's real Meta spend approaches `budget_cap`, when reconciliation runs, then Meta's own budget control (Ad Set-level spend cap) plus Adren's polling-based `SpendCapReached` transition together ensure the campaign never meaningfully overshoots, even under processing lag.

## Developer Notes
- **PRD reference(s):** §14.3 Controls & Guardrails; §24.6 NFR Ads/Campaign
- **Module(s)/Screen(s):** ads
- **Story points:** 8 — Reconciling two independent spend-tracking sources (Meta's own controls + Adren's polling) with a real-money liability concern makes this the highest-stakes MADS story.
- **Dependencies:** MADS-04, ADS-10
- **Testing tier(s):** unit, integration (Testcontainers)

## Sub-tasks
- [EXTEND] Backend: Meta Ad Set-level budget cap set at launch (MADS-02)
- [EXTEND] Backend: `SpendCapReached` reconciliation against real Meta spend data
- [NEW] Backend: unit test
- [NEW] Backend: integrationTest — simulated near-cap spend against Meta's sandbox
