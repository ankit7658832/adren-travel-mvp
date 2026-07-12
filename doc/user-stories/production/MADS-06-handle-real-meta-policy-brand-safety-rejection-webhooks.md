---
id: MADS-06
epic: Meta Ads API Real Integration
phase: production
status: not-started
story_points: 5
dependencies: ["MADS-02", "ADS-15"]
labels: ["backend", "ads", "phase2"]
prd_references: ["§14.2"]
modules_or_screens: ["ads"]
testing_tiers: ["unit", "module (@ApplicationModuleTest)"]
---

# MADS-06: Handle real Meta policy/brand-safety rejection webhooks

## Summary (business)
If Facebook/Instagram itself blocks or rejects a campaign for violating their advertising rules (separate from any checks Adren already does), Adren will now detect this and clearly inform the consultant why their ad was turned down. This prevents confusion where a campaign appears stuck with no explanation.

## User Story
**As a** Super Admin, **I want** be notified when Meta itself rejects a campaign for policy reasons, distinct from Adren's own pre-check (ADS-15), **so that** PRD §14.2 step 5's review gate accounts for Meta-side rejection, not just Adren's internal policy review.

## Acceptance Criteria
- Given Meta rejects a launched campaign for a policy violation Adren's own pre-check (ADS-15) didn't catch, when the rejection webhook is received, then the campaign's status reflects the Meta-side rejection with the reason surfaced to the Consultant, distinct from an Adren-internal Rejected status.

## Developer Notes
- **PRD reference(s):** §14.2 Flow step 5
- **Module(s)/Screen(s):** ads
- **Story points:** 5 — Webhook-handling integration; the distinct-status requirement is the specific correctness bar.
- **Dependencies:** MADS-02, ADS-15
- **Testing tier(s):** unit, module (@ApplicationModuleTest)

## Sub-tasks
- [NEW] Backend: Meta policy-rejection webhook handler
- [NEW] Backend: unit test
- [NEW] Backend: module test
