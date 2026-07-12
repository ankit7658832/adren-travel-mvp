---
id: FES-10
epic: Frontend Shell
phase: mock
status: not-started
story_points: 3
dependencies: ["FES-03"]
labels: ["frontend", "foundation", "phase1"]
prd_references: ["§21"]
modules_or_screens: ["shared (frontend)"]
testing_tiers: ["component test"]
---

# FES-10: Add a global toast/notification queue for async operation feedback

## Summary (business)
This adds a consistent, platform-wide way to show users pop-up confirmation or error messages after actions like saving, publishing, or making a payment, instead of each screen inventing its own version. This gives users clear, reliable feedback everywhere in the product, including for users relying on screen readers, which improves trust and reduces confusion during important actions like payments.

## User Story
**As a** Consultant/User, **I want** see a toast confirmation or error for async operations (save, publish, payment) across any screen, **so that** async feedback is consistent platform-wide rather than each feature inventing its own transient-message pattern.

## Acceptance Criteria
- Given an async mutation succeeds or fails on any screen, when the result resolves, then a toast is queued via the shared Zustand-backed toast store and displayed with an appropriate ARIA live region per RULES.md §7.3.

## Developer Notes
- **PRD reference(s):** §21 (cross-screen UX consistency)
- **Module(s)/Screen(s):** shared (frontend)
- **Story points:** 3 — Small, well-scoped Zustand store (cross-cutting client state per RULES.md §7.1) plus a toast-rendering component.
- **Dependencies:** FES-03
- **Testing tier(s):** component test

## Sub-tasks
- [NEW] Frontend: `toastQueueStore` (Zustand)
- [NEW] Frontend: `ToastContainer` component with ARIA live region
- [NEW] Frontend: component test — queued toast renders and auto-dismisses
