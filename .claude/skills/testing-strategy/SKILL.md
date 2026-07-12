---
name: testing-strategy
description: Test-tier conventions for both backend (unit / @ApplicationModuleTest / Testcontainers integrationTest) and frontend (co-located component tests / Playwright e2e) — which tier to use for what, coverage gates, how to run each. Use before adding a new test file or deciding how to test a new module/feature.
metadata:
  type: project-skill
---

Mechanics and tier selection. For *what must be true* about the code under test (transaction boundaries, idempotency, tenant isolation) see `doc/architecture/RULES.md` — a green test suite doesn't substitute for those rules being followed; write tests that would actually catch a violation (e.g., a test asserting a second Consultant cannot read the first Consultant's itinerary, not just a happy-path test).

## Backend — three tiers

| Tier | Location | Tool | What it covers | Docker? |
|---|---|---|---|---|
| Unit | `src/test/java`, plain classes (e.g. `MoneyTest`, `BookingServiceImplTest`) | JUnit 5 + Mockito | Pure logic, entity business rules, service logic with mocked repos/publishers | No |
| Module slice | `src/test/java`, `@ApplicationModuleTest` + Modulith's `Scenario` API | Spring Modulith test support | One module in isolation — verifies event publication/consumption without booting unrelated modules | No (embedded) |
| End-to-end | `src/integrationTest/java` (separate Gradle source set) | `@SpringBootTest` + Testcontainers (Postgres 16-alpine, LocalStack) | Full stack through real Postgres/AWS-service-emulation, cross-module flows | Yes |

**Choosing a tier:** default to unit for anything that doesn't need a Spring context. Reach for `@ApplicationModuleTest` when you're testing that a module publishes the right event or reacts to one correctly, without needing the whole app wired up. Reserve Testcontainers `integrationTest` for flows that genuinely need a real database or a real (LocalStack-emulated) AWS service — a full itinerary→quotation→booking round trip, a Flyway migration correctness check, anything exercising `@Transactional` + the JPA event publication registry together (that combination is exactly what §4.3/§2.2 of `RULES.md` need proven, not just asserted in a unit test with mocks).

```bash
./gradlew test                # unit + @ApplicationModuleTest slices — fast, run this constantly
./gradlew integrationTest      # Testcontainers-backed — requires Docker running
./gradlew check                # both, plus ModularityTests boundary verification
```

Supplier integration tests: per PRD §23.2 Edge Case #4, sandbox and production supplier environments can behave differently (Hotelbeds/TBO specifically called out) — when a real supplier client replaces a stub, its integration tests should run against both sandbox and (where safely possible) production-like fixtures, flagged separately in CI rather than assumed equivalent.

## Frontend — two tiers

| Tier | Location | Tool | What it covers |
|---|---|---|---|
| Unit/component | co-located, `Component.test.tsx` / `useHook.test.ts` | Vitest + Testing Library (jsdom) | Component rendering/interaction, hook state machines — fast, run on every save |
| E2E | `e2e/*.spec.ts` | Playwright, real Chromium | Highest-value user journeys only — PRD §9.1 Flow A/B/C (search→itinerary→quotation, package creation, direct booking) |

```bash
npm run test           # Vitest — unit + component, jsdom
npm run test:coverage  # same, coverage thresholds enforced in vite.config.ts (currently lines/functions/statements 70%, branches 60% — a floor, raise as real feature coverage grows past scaffold stage)
npm run test:e2e       # Playwright — auto-starts dev server, single chromium project
```

Component tests assert on user-visible behavior (`getByRole`, `getByLabelText`, `getByText`) — not implementation details like internal state shape or hook call counts. `SearchDashboard.test.tsx`/`useMultiLocationSearch.test.ts` are the reference pattern (`renderHook`/`act`/`waitFor` for the hook, full-tree `render`/`screen`/`fireEvent` for the component).

`msw` is an installed devDependency but not yet wired into `src/test/setup.ts` (which currently only registers jest-dom matchers) — once a feature starts making real `apiClient` calls instead of a mocked hook function (see `RULES.md` §7.1's React Query reconciliation), set up an MSW server in `setup.ts` to intercept those calls in tests rather than mocking `apiClient` itself, so tests exercise the real request/response shape.

Reserve e2e for journeys, not screens — a new screen gets a component test by default; only add/extend an e2e spec when the screen is part of one of the three PRD §9.1 flows and the thing being verified is genuinely cross-screen (can't be caught by component tests in isolation).

## New-module/feature testing checklist

- [ ] Backend: business-rule-bearing logic (entity state transitions, service methods) has a unit test.
- [ ] Backend: any module that publishes/consumes an event has an `@ApplicationModuleTest` covering it.
- [ ] Backend: any flow crossing `@Transactional` + event publication has a Testcontainers `integrationTest` proving the outbox atomicity, not just a unit test with a mocked publisher.
- [ ] Frontend: new component has a co-located test covering all implemented PRD Part 21 states (success/empty/error/loading), not just happy path.
- [ ] Frontend: e2e spec added/extended only if the screen is part of an §9.1 Flow A/B/C journey.
