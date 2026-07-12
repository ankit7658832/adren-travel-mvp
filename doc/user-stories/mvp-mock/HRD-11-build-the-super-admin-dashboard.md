---
id: HRD-11
epic: Hardening
phase: mock
status: not-started
story_points: 8
dependencies: ["HRD-09", "AI-11", "FND-02"]
labels: ["backend", "frontend", "phase1"]
prd_references: ["§9.5", "§21.6"]
modules_or_screens: ["booking", "supplier", "ai", "ads", "Super Admin Console (21.6)"]
testing_tiers: ["unit", "module (@ApplicationModuleTest)", "component test"]
---

# HRD-11: Build the Super Admin Dashboard

## Summary (business)
Company administrators get a top-level reporting dashboard showing total sales across all consultants, how each travel supplier is performing, a summary of AI-related oversight activity, and advertising spend company-wide, giving leadership the full business picture in one view.

## User Story
**As a** Super Admin, **I want** see all-Consultant GMV, supplier performance, AI governance summary, and ad spend across Consultants, **so that** PRD §9.5 and §21.6's global reporting spec are implemented.

## Acceptance Criteria
- Given Super Admin opens Global Reporting, when the page loads, then all-Consultant GMV, per-supplier performance, an AI governance summary, and ad spend across Consultants are all shown, scoped to the SUPER_ADMIN 'view all' path.

## Developer Notes
- **PRD reference(s):** §9.5 Reporting & Dashboard Spec; §21.6 Super Admin Console (Global Reporting)
- **Module(s)/Screen(s):** booking, supplier, ai, ads, Super Admin Console (21.6)
- **Story points:** 8 — Cross-module aggregation at platform scope (not per-tenant) — the Super Admin equivalent of HRD-09, broader in module reach.
- **Dependencies:** HRD-09, AI-11, FND-02
- **Testing tier(s):** unit, module (@ApplicationModuleTest), component test

## Sub-tasks
- [NEW] Backend: `GET /api/v1/dashboard/super-admin` composite read endpoint, SUPER_ADMIN-only
- [NEW] Frontend: `useSuperAdminDashboard` hook (React Query for server data per RULES.md §7.1)
- [NEW] Frontend: `SuperAdminDashboard.tsx` component — all 5 PRD Part 21 states (default/loading/success/empty/error)
- [NEW] Frontend: component test (Testing Library, co-located, asserts on role/label per RULES.md §7.3)
