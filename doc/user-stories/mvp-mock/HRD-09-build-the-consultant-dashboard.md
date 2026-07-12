---
id: HRD-09
epic: Hardening
phase: mock
status: not-started
story_points: 8
dependencies: ["BOK-12", "FIN-06", "ADS-09", "FND-23"]
labels: ["backend", "frontend", "phase1"]
prd_references: ["§9.5", "§21.5"]
modules_or_screens: ["booking", "payments", "ads", "Consultant Dashboard (21.5) — NEW feature folder"]
testing_tiers: ["unit", "module (@ApplicationModuleTest)", "component test", "e2e"]
---

# HRD-09: Build the Consultant Dashboard

## Summary (business)
Consultants get a home dashboard showing their bookings for the month, best-selling packages, available account balance, quotations awaiting response, and any marketing campaigns currently running, giving them a quick, complete view of their business performance in one place.

## User Story
**As a** Consultant, **I want** see bookings this month, top packages, wallet balance, pending quotations, and active campaigns in one place, **so that** PRD §9.5 and §21.5's dashboard spec are implemented.

## Acceptance Criteria
- Given a Consultant with existing activity opens their dashboard, when the page loads, then summary cards (bookings this month, GMV, wallet balance) and tabs (Top Packages, Pending Quotations, Active Campaigns) are all populated from real data.

## Developer Notes
- **PRD reference(s):** §9.5 Reporting & Dashboard Spec; §21.5 Consultant Dashboard
- **Module(s)/Screen(s):** booking, payments, ads, Consultant Dashboard (21.5) — NEW feature folder
- **Story points:** 8 — Aggregates read models across three modules (booking, payments, ads) into one dashboard — the broadest single-screen data-fetching surface in the catalogue.
- **Dependencies:** BOK-12, FIN-06, ADS-09, FND-23
- **Testing tier(s):** unit, module (@ApplicationModuleTest), component test, e2e

## Sub-tasks
- [NEW] Backend: `GET /api/v1/dashboard/consultant` composite read endpoint (paginated sub-collections)
- [NEW] Frontend: `useConsultantDashboard` hook (React Query for server data per RULES.md §7.1)
- [NEW] Frontend: `ConsultantDashboard.tsx` component — all 5 PRD Part 21 states (default/loading/success/empty/error)
- [NEW] Frontend: component test (Testing Library, co-located, asserts on role/label per RULES.md §7.3)
- [NEW] Frontend: Playwright e2e spec (extends `search-flow.spec.ts` pattern, PRD §9.1 flow)
