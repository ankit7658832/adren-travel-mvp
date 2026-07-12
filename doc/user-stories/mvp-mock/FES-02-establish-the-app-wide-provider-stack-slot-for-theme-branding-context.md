---
id: FES-02
epic: Frontend Shell
phase: mock
status: not-started
story_points: 3
dependencies: []
labels: ["frontend", "foundation", "phase1"]
prd_references: ["§13.2"]
modules_or_screens: ["Frontend shell (provider stack)"]
testing_tiers: ["unit"]
---

# FES-02: Establish the app-wide provider stack slot for theme/branding context

## Summary (business)
This establishes a single, agreed-upon place in the application's setup where app-wide features like branding and login/session handling get plugged in. Doing this now prevents future development teams from each choosing their own approach, which reduces bugs and rework as more features are added.

## User Story
**As a** frontend engineer, **I want** have a defined slot between `QueryClientProvider` and `BrowserRouter` for app-wide providers, **so that** theme/branding (FES-06) and auth (FES-07) context land in the same established position rather than each PR guessing where to put its provider.

## Acceptance Criteria
- Given a new app-wide provider is added, when it is wired into `main.tsx`, then it sits between `QueryClientProvider` and `BrowserRouter` per the documented convention, unless it specifically needs router context.

## Developer Notes
- **PRD reference(s):** §13.2 Branding Configuration
- **Module(s)/Screen(s):** Frontend shell (provider stack)
- **Story points:** 3 — Small, mechanical, but establishes a convention every subsequent provider-adding story depends on.
- **Dependencies:** None
- **Testing tier(s):** unit

## Sub-tasks
- [EXTEND] Frontend: `main.tsx` provider stack — documented slot + placeholder composition helper
- [NEW] Frontend: unit test — provider order asserted
