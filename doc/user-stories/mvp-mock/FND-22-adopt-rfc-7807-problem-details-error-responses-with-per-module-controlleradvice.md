---
id: FND-22
epic: Foundation
phase: mock
status: not-started
story_points: 5
dependencies: ["FND-21"]
labels: ["backend", "foundation", "phase1"]
prd_references: ["§3.3"]
modules_or_screens: ["shared", "booking"]
testing_tiers: ["unit", "module (@ApplicationModuleTest)"]
---

# FND-22: Adopt RFC 7807 Problem Details error responses with per-module @ControllerAdvice

## Summary (business)
This story standardizes the format of error messages the platform sends back when something fails, so every part of the system reports problems in a consistent, easy-to-understand way, including a reference ID for troubleshooting. This lets the frontend team build one reliable way of showing errors to users instead of handling each type of failure differently.

## User Story
**As a** API consumer (frontend engineer), **I want** receive a consistent error shape from every endpoint, including a traceId and field-level errors where applicable, **so that** the frontend can build one error-handling code path instead of reverse-engineering each module's ad hoc error format, per RULES.md §3.3.

## Acceptance Criteria
- Given any endpoint returns a 4xx/5xx, when the response body is inspected, then it matches the RFC 7807 shape (`type`, `title`, `status`, `detail`, `instance`, `traceId`) with `errors[]` present only for field-level validation failures.
- Given a `@Valid` Bean Validation failure occurs, when the response is built, then `errors[]` lists the specific field(s) and message(s).

## Developer Notes
- **PRD reference(s):** §3.3 Error response shape (RULES.md)
- **Module(s)/Screen(s):** shared, booking
- **Story points:** 5 — New shared `ProblemDetailFactory` plus one `@ControllerAdvice` per existing module (booking today, more as modules land).
- **Dependencies:** FND-21
- **Testing tier(s):** unit, module (@ApplicationModuleTest)

## Sub-tasks
- [NEW] Backend: `shared.ProblemDetailFactory` (shape only, per RULES.md — not per-module type/title catalogue)
- [NEW] Backend: `booking`'s `@ControllerAdvice` using the factory
- [NEW] Backend: unit test — validation failure produces `errors[]`
- [NEW] Backend: module test — traceId matches the request's generated ID
