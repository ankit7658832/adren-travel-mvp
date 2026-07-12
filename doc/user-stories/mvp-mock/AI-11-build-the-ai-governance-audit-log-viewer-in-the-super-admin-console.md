---
id: AI-11
epic: AI Layer
phase: mock
status: not-started
story_points: 5
dependencies: ["AI-07", "AI-08", "FND-02"]
labels: ["backend", "frontend", "ai", "phase1"]
prd_references: ["§6", "§21.6"]
modules_or_screens: ["ai", "Super Admin Console (21.6)"]
testing_tiers: ["unit", "module (@ApplicationModuleTest)", "component test"]
---

# AI-11: Build the AI Governance/Audit Log viewer in the Super Admin Console

## Summary (business)
This builds a screen for senior administrators to review the complete history of every AI suggestion made across all consultants in the business, with the ability to filter and search. It gives leadership the oversight needed to monitor AI quality, investigate issues, and demonstrate responsible AI use, while ensuring only authorized senior staff (not individual consultants) can see the full company-wide picture.

## User Story
**As a** Super Admin, **I want** browse the AI suggestion audit log across all Consultants, **so that** PRD §6's 'View AI governance/audit logs (Yes, all)' capability and §21.6's Super Admin Console navigation are implemented.

## Acceptance Criteria
- Given Super Admin opens the AI Governance Logs section, when the page loads, then every AI suggestion's input, source data, output, and disposition is browsable and paginated, filterable by Consultant.
- Given a Consultant attempts to reach the equivalent view, when the request is made, then it is rejected — only Super Admin has 'all' visibility per §6.

## Developer Notes
- **PRD reference(s):** §6 Roles & Permissions Matrix; §21.6 Super Admin Console
- **Module(s)/Screen(s):** ai, Super Admin Console (21.6)
- **Story points:** 5 — Read-only UI over AI-07/AI-08's audit trail with pagination and role-gating.
- **Dependencies:** AI-07, AI-08, FND-02
- **Testing tier(s):** unit, module (@ApplicationModuleTest), component test

## Sub-tasks
- [NEW] Backend: `GET /api/v1/ai/audit-log` paginated, Super-Admin-only endpoint
- [NEW] Frontend: `useAiGovernanceLog` hook (React Query for server data per RULES.md §7.1)
- [NEW] Frontend: `AiGovernanceLogViewer.tsx` component — all 5 PRD Part 21 states (default/loading/success/empty/error)
- [NEW] Frontend: component test (Testing Library, co-located, asserts on role/label per RULES.md §7.3)
