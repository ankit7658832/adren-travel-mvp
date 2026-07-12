---
name: backend-spring-modulith
description: Mechanics of adding/changing a backend module in the Spring Modulith monolith — package shape, module boundary enforcement, event publication, running the modularity check. Use before creating a new module (ai/payments/whitelabel/ads/compliance) or wiring any cross-module call.
metadata:
  type: project-skill
---

Lean, mechanics-focused. For the *reasoning* behind these rules — why boundaries matter here, what a violation looks like in review, event-vs-API-call decisions, transaction/event schema rules — see `doc/architecture/RULES.md` §1–2. This skill is the "how," that doc is the "why." For Java-level conventions inside a module (SOLID/DDD, exceptions, concurrency, N+1 avoidance) see the `backend-best-practices` skill. For test-tier mechanics see `testing-strategy`.

## Module package shape

Every module under `backend/src/main/java/com/adren/travel/` follows this exact shape (copy `booking/` or `supplier/` as the template, not `ai`/`payments`/`whitelabel`/`ads`/`compliance` — those are still package-info-only stubs):

```
<module>/
├── package-info.java        @ApplicationModule(displayName = "...")
├── <Module>Api.java          public interface — the ONLY class other modules call
├── event/
│   ├── package-info.java     @NamedInterface("event")
│   └── <Something>Event.java records other modules may @ApplicationModuleListener
└── internal/
    ├── <Entity>.java          @Entity, package-private
    ├── <Entity>Repository.java  extends JpaRepository, package-private
    ├── <Module>ServiceImpl.java  @Service implements <Module>Api, package-private
    └── <Module>Controller.java   @RestController, injects the Api interface only
```

- `package-info.java` type defaults to `ApplicationModule.Type.ENCAPSULATED` (omit `type=`). Only `shared` is `OPEN`.
- Everything in `internal/` is package-private unless a framework (JPA) forces otherwise. Public-in-`internal` is not an invitation to import it from another module.
- If a module needs to expose something other than `<Module>Api`, declare a new `@NamedInterface`-annotated subpackage for it (mirrors `booking.event`) — don't just make another top-level class public.

## Wiring a new module

1. Copy the shape above into `backend/src/main/java/com/adren/travel/<module>/`.
2. Write `<Module>Api.java` first — it's the contract other modules see. Keep it small; add methods when a real caller needs them, not speculatively.
3. Add a Flyway migration under `backend/src/main/resources/db/migration/V<n>__<module>_init.sql` for any new entity's table (module owns its own tables — no cross-module shared tables).
4. If other modules need to react to something this module does, add a record under `<module>/event/` and publish it via `ApplicationEventPublisher.publishEvent(...)` from inside a `@Transactional` service method (see `RULES.md` §4.3 for why the transaction boundary matters).
5. Consume another module's events with `@ApplicationModuleListener` on a method taking that module's event type, importing only from `<other-module>.event`, never `<other-module>.internal`.

## Verifying boundaries before you push

```bash
cd backend
./gradlew check          # runs test + integrationTest + ModularityTests.moduleBoundariesAreRespected()
```

`ModularityTests` (`backend/src/test/java/com/adren/travel/ModularityTests.java`) calls `ApplicationModules.of(AdrenTravelApplication.class).verify()`. This fails the build on: an illegal `.internal` import from outside its module, or a cyclic module dependency. Run `./gradlew check`, not just `./gradlew test` — the modularity check is not part of the default `test` task.

## Regenerating module docs

```bash
./gradlew test --tests ModularityTests
cp build/spring-modulith-docs/*.puml ../doc/architecture/
```

`writeModuleDocumentation()` generates PlantUML diagrams straight from the code structure. Regenerate and copy them into `doc/architecture/` as part of the release checklist so the module map can't drift from reality — see `doc/README.md`.

## Adding a new module — checklist

- [ ] Package shape matches the template above (`Api` + `event` + `internal`).
- [ ] `package-info.java` has `@ApplicationModule(displayName = "...")`, no `type = OPEN` without discussing it against `RULES.md` §1.3.
- [ ] New entities are package-private, own their own Flyway-migrated tables.
- [ ] `./gradlew check` passes locally before pushing.
- [ ] `backend/README.md`'s module table status column updated (stub → reference implementation).
