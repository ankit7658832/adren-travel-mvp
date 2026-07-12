---
id: ADS-11
epic: Ads/Campaign Management
phase: mock
status: not-started
story_points: 5
dependencies: ["ADS-10"]
labels: ["backend", "frontend", "ads", "phase1"]
prd_references: ["§14.3"]
modules_or_screens: ["ads"]
testing_tiers: ["unit", "module (@ApplicationModuleTest)", "component test"]
---

# ADS-11: Implement campaign approval workflow guardrails and billing transparency

## Summary (business)
Consultants get full, itemized visibility into what they're being charged for ad spend - not just a single lump total - alongside consistent safeguards (budget limits, required approvals, brand-safety checks, and account-suspension warnings) throughout the advertising process. This builds trust by making advertising costs transparent and keeping the whole process well-controlled end to end.

## User Story
**As a** Consultant, **I want** see exactly what I'm being billed for ad spend, with clear approval-workflow guardrails throughout, **so that** PRD §14.3's full guardrail set (spend caps, approval workflow, brand-safety review, billing transparency, account-suspension escalation) is implemented as a cohesive whole beyond the individual state transitions.

## Acceptance Criteria
- Given a Consultant views a Live campaign, when they open the billing detail, then spend-to-date, budget cap, and a per-transaction breakdown are all visible, not summarized into one opaque figure.

## Developer Notes
- **PRD reference(s):** §14.3 Controls & Guardrails
- **Module(s)/Screen(s):** ads
- **Story points:** 5 — Consolidates ADS-05/06/10's individual guardrails into a coherent billing-transparency view.
- **Dependencies:** ADS-10
- **Testing tier(s):** unit, module (@ApplicationModuleTest), component test

## Sub-tasks
- [NEW] Backend: `GET /api/v1/campaigns/{id}/billing-detail` endpoint
- [NEW] Frontend: `useCampaignBillingDetail` hook (React Query for server data per RULES.md §7.1)
- [NEW] Frontend: `CampaignBillingDetail.tsx` component — all 5 PRD Part 21 states (default/loading/success/empty/error)
- [NEW] Frontend: component test (Testing Library, co-located, asserts on role/label per RULES.md §7.3)
