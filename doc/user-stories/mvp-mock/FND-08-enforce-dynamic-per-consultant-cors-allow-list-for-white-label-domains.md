---
id: FND-08
epic: Foundation
phase: mock
status: not-started
story_points: 5
dependencies: ["FND-06"]
labels: ["backend", "foundation", "security", "phase1"]
prd_references: ["§5.4", "§13.2"]
modules_or_screens: ["whitelabel", "shared"]
testing_tiers: ["unit", "module (@ApplicationModuleTest)"]
---

# FND-08: Enforce dynamic per-Consultant CORS allow-list for white-label domains

## Summary (business)
This story ensures that only a consultant's approved, registered website address is allowed to talk to the platform's backend, and any unrecognized website is blocked. This closes a security gap where an unauthorized website could otherwise impersonate a consultant's storefront and steal customer sessions or data.

## User Story
**As a** platform security owner, **I want** have CORS resolved per-request from the whitelabel domain registry, **so that** a wildcard CORS policy never becomes the default path to cross-tenant credential/session theft, per RULES.md §5.4.

## Acceptance Criteria
- Given a request originates from a Consultant's mapped CNAME domain, when CORS is evaluated, then it is allowed.
- Given a request originates from an unmapped/unknown origin, when CORS is evaluated, then it is rejected — no wildcard fallback exists anywhere in config.

## Developer Notes
- **PRD reference(s):** §5.4 OWASP concerns (RULES.md); §13.2 Branding Configuration
- **Module(s)/Screen(s):** whitelabel, shared
- **Story points:** 5 — Security-sensitive but well-scoped: one CORS filter reading FND-06's domain registry.
- **Dependencies:** FND-06
- **Testing tier(s):** unit, module (@ApplicationModuleTest)

## Sub-tasks
- [NEW] Backend: dynamic CORS filter resolving allow-list from `whitelabel` domain registry
- [NEW] Backend: unit test — mapped vs. unmapped origin
- [NEW] Backend: module test — no wildcard present in any active profile
