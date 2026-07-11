# ADREN TRAVEL — Development Monorepo

Scaffold for MVP mock development, matching `doc/PRD_v2_detailed.md`.

```
adren-travel/
├── backend/          Spring Boot 3 + Spring Modulith + Gradle (Kotlin DSL)
├── frontend/         React 18 + Vite + TypeScript
├── doc/              PRDs + auto-generated architecture diagrams
└── .claude/skills/   Claude Code skills for this repo (see below)
```

## Quick start

```bash
# Backend
cd backend
docker compose up -d       # Postgres + LocalStack
./gradlew bootRun

# Frontend (separate terminal)
cd frontend
npm install
npm run dev
```

## Claude Code skills

Three skills live in `.claude/skills/` and load automatically when Claude
Code works in this repo:

| Skill | Covers |
|---|---|
| `backend-spring-modulith` | Module boundaries, event-driven cross-module wiring, adding endpoints/entities/suppliers |
| `frontend-react-vite` | Screen structure, required UI states, data fetching, component conventions |
| `testing-strategy` | The four-tier test pyramid, which tier to use, mapping PRD acceptance criteria to test cases |

These encode the conventions demonstrated in the reference implementation
(the `booking`, `supplier`, and `notification` modules on the backend; the
`search-dashboard` feature on the frontend) — read them before extending
any other module/screen so new code matches the established pattern rather
than inventing a new one per feature.

## What's implemented vs. stubbed

This is a **structural scaffold**, not a working MVP. Fully implemented as
reference patterns: the Booking module's Quotation/Booking-confirmation
flow with real event publication, the Hotelbeds supplier client (stubbed
HTTP call, real integration contract), and the Search Dashboard screen with
all four UI states. Everything else (`ai`, `payments`, `whitelabel`, `ads`,
`compliance` backend modules; all other frontend screens) is either a
package-info stub or not yet started — build them out following the
patterns in the two reference modules/screens above, guided by the
Claude Code skills.

## Next step: user stories

Per the earlier estimate (~135-165 MVP user stories across 15 epics), start
with the **Platform Foundation** and **Supplier Integration** epics first —
everything else depends on them. Each story should reference:
- The relevant PRD section (`doc/PRD_v2_detailed.md`)
- The module(s) it touches (`backend/README.md`'s status table)
- Its acceptance criteria (PRD Part 22, or PRD Part 25's test scenario IDs
  where applicable)
