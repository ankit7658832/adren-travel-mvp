---
id: MADS-08
epic: Meta Ads API Real Integration
phase: production
status: not-started
story_points: 8
dependencies: ["MADS-01", "MADS-05"]
labels: ["backend", "ads", "security", "phase2"]
prd_references: ["§7"]
modules_or_screens: ["ads"]
testing_tiers: ["integration (Testcontainers)"]
---

# MADS-08: Isolate Meta Business Manager billing/liability per Consultant at production scale

## Summary (business)
This story puts safeguards in place so that even though Adren manages many consultants' advertising accounts under one central Facebook/Instagram business setup, each consultant's ad spending and billing stays completely separate and cannot accidentally get mixed up with another consultant's charges. This protects both Adren and consultants from billing errors or disputes.

## User Story
**As a** Super Admin, **I want** be certain one Consultant's ad spend/billing can never bleed into another Consultant's account under the shared Adren umbrella, **so that** PRD §7's named risk ('ad account liability — Adren manages Meta accounts/billing on Consultants' behalf') is mitigated with real controls, not just documented as a risk.

## Acceptance Criteria
- Given two Consultants each have a Meta ad account provisioned under Adren's Business Manager, when billing is reconciled, then each Consultant's spend is fully attributable and isolated — no cross-Consultant billing leakage is possible even under a shared umbrella structure.

## Developer Notes
- **PRD reference(s):** §7 Assumptions, Dependencies, Risks
- **Module(s)/Screen(s):** ads
- **Story points:** 8 — Billing-isolation guarantee across a shared third-party account structure — genuinely hard multi-tenancy problem at the Meta-account level, not just Adren's own database.
- **Dependencies:** MADS-01, MADS-05
- **Testing tier(s):** integration (Testcontainers)

## Sub-tasks
- [NEW] Backend: per-Consultant Meta sub-account/campaign-budget isolation model
- [NEW] Backend: billing reconciliation audit — cross-Consultant leakage check
- [NEW] Backend: integrationTest — two-Consultant billing isolation scenario
