---
id: FND-01
epic: Foundation
phase: mock
status: not-started
story_points: 8
dependencies: []
labels: ["backend", "foundation", "security", "phase1"]
prd_references: ["§5.1", "§6"]
modules_or_screens: ["shared (cross-cutting security infra)"]
testing_tiers: ["unit", "module (@ApplicationModuleTest)"]
---

# FND-01: Stand up stateless Spring Security with role- and tenant-aware principal

## Summary (business)
This story adds basic login security so that only verified users can access the platform. Right now anyone could reach the system without proving who they are, which is a serious risk; going forward every request must include a valid login token that identifies the person and their role before anything happens.

## User Story
**As a** backend engineer, **I want** authenticate every request and attach a principal carrying user ID, role, and consultant_id, **so that** every module can enforce the PRD §6 role matrix instead of the platform having zero authorization as it does today.

## Acceptance Criteria
- Given an unauthenticated request hits any non-public endpoint, when it is received, then the platform returns 401 rather than serving the request.
- Given a valid JWT is presented, when the request is processed, then the principal exposes userId, role (SUPER_ADMIN/CONSULTANT/USER), and consultant_id (null only for SUPER_ADMIN).

## Developer Notes
- **PRD reference(s):** §5.1 (RULES.md); §6 Roles & Permissions Matrix
- **Module(s)/Screen(s):** shared (cross-cutting security infra)
- **Story points:** 8 — Net-new cross-cutting infra (JWT filter chain, principal model) touching every future controller — highest-risk foundational piece.
- **Dependencies:** None
- **Testing tier(s):** unit, module (@ApplicationModuleTest)

## Sub-tasks
- [NEW] Backend: `SecurityConfig` + JWT filter chain (stateless, no session)
- [NEW] Backend: `AdrenPrincipal` carrying userId/role/consultantId
- [NEW] Backend: unit test (filter chain, token parsing, 401/403 paths)
- [NEW] Backend: module test (principal available to a sample secured endpoint)
