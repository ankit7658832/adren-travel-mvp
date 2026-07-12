---
id: AI-10
epic: AI Layer
phase: mock
status: not-started
story_points: 5
dependencies: ["AI-02", "AI-04", "AI-05", "FND-16"]
labels: ["frontend", "ai", "phase1"]
prd_references: ["§21.2", "§11.2"]
modules_or_screens: ["Itinerary Builder (21.2)"]
testing_tiers: ["component test", "e2e"]
---

# AI-10: Build the 'Complete with AI' entry point with source/availability badges

## Summary (business)
This delivers the actual on-screen 'Complete with AI' button that consultants use, where every AI suggestion is visibly tagged with its supplier and how current its availability is, and nothing is added to the trip automatically - the consultant must actively accept or reject each suggestion. This keeps consultants firmly in control while still benefiting from AI speed.

## User Story
**As a** Consultant/User, **I want** invoke AI assistance and see AI-suggested line items with source-supplier and availability badges before they're added — never silently inserted, **so that** PRD §21.2's AI-assist entry point spec is implemented exactly as described.

## Acceptance Criteria
- Given 'Complete with AI' is clicked, when suggestions are returned, then they are shown with source-supplier and availability badges (AI-04's fields) and require explicit accept/reject — never silently inserted into the itinerary.

## Developer Notes
- **PRD reference(s):** §21.2 Itinerary Builder (AI-assist entry point); §11.2 principle 2
- **Module(s)/Screen(s):** Itinerary Builder (21.2)
- **Story points:** 5 — Frontend consumer of AI-02/AI-04/AI-05's typed response states (Suggested/NoViableSuggestion) — the UI must render all three explicitly.
- **Dependencies:** AI-02, AI-04, AI-05, FND-16
- **Testing tier(s):** component test, e2e

## Sub-tasks
- [NEW] Frontend: `useAiAssist` hook (React Query for server data per RULES.md §7.1)
- [NEW] Frontend: `AiAssistPanel.tsx` component — all 5 PRD Part 21 states (default/loading/success/empty/error)
- [NEW] Frontend: component test (Testing Library, co-located, asserts on role/label per RULES.md §7.3)
- [NEW] Frontend: Playwright e2e spec (extends `search-flow.spec.ts` pattern, PRD §9.1 flow)
