
# ADREN TRAVEL — Architecture Rules

**Audience:** backend and frontend engineers, reviewers. **Status:** living document — update it in the same PR that changes a convention, not after.

This document is the *why* behind the conventions already visible in `booking/`, `supplier/`, `notification/`, `shared/` (backend) and `search-dashboard/` (frontend). It does not repeat command-line mechanics — that's what the Claude Code skills are for:

| Skill | Covers |
|---|---|
| `backend-spring-modulith` | Module scaffolding mechanics, `ModularityTests`, how to add a module |
| `frontend-react-vite` | Feature-folder mechanics, screen states, routing/provider setup |
| `testing-strategy` | Test tiers, when to write which kind of test, coverage gates |
| `backend-best-practices` | Java/SOLID/DDD conventions, exception/concurrency/DI/perf patterns |
| `frontend-best-practices` | Component design, performance, error boundaries, forms, design system |

Read this document for *rules and reasoning*; read the skills for *how to execute them*. Where a rule below conflicts with code already in the repo, it's called out explicitly in a **⚠️ Reconcile** box — these are known gaps, not hidden contradictions, and should be the first backlog items pulled off this doc.

---

## 0. How to use this document in a PR

Every PR touching `backend/` or `frontend/` should be checkable against §8 (the checklist) in under two minutes. If a rule doesn't apply, say why in the PR description rather than silently skipping it — that's what keeps this document trustworthy instead of decorative.

---

## 1. Module Boundary Rules (Spring Modulith)

**Why boundaries matter here specifically:** ADREN's module list (`booking`, `supplier`, `ai`, `payments`, `whitelabel`, `ads`, `notification`, `compliance`) is really nine separate bounded contexts sharing one deployable. Spring Modulith gives us the enforcement mechanism (`ApplicationModules.verify()` in `ModularityTests`) that a microservices split would get for free from network boundaries. If we let that discipline slip, we end up with a distributed monolith's problems (hidden coupling) without a monolith's simplicity. Every module boundary decision should be made as if it might become a network boundary in a future extraction — because for a platform this compliance-heavy (six jurisdictions, Part 17), a hard split of `payments` or `compliance` is a realistic future, not a hypothetical.

### 1.1 The module shape is not optional

Every module — existing or new (`ai`, `payments`, `whitelabel`, `ads`, `compliance` are currently package-info-only stubs) — must follow the shape already established by `booking` and `supplier`:

```
<module>/
├── package-info.java        (@ApplicationModule — ENCAPSULATED unless justified, see 1.3)
├── <Module>Api.java         (the ONLY class other modules may call directly)
├── event/                   (@NamedInterface("event") — records other modules may @ApplicationModuleListener)
└── internal/                (entities, repositories, service impls, controllers — package-private by default)
```

Rule: if a class in `internal/` needs to be `public` for a framework reason (JPA sometimes requires it), that is not license to import it from another module. `ModularityTests.verify()` will catch a cross-module `.internal` import at build time — but don't rely on CI to be the first line of defense. A reviewer should reject any import matching `com\.adren\.travel\.\w+\.internal` that originates outside that module's own package, on sight, before running the build.

### 1.2 What a boundary violation looks like in review

Watch for these specific patterns — they're the realistic ways a boundary gets breached, not the obvious ones:

1. **Direct entity/repository reuse across modules.** E.g., `payments` autowiring `ItineraryRepository` to look up a total instead of calling `BookingApi`. This is the single most common Modulith violation in practice because it's the path of least resistance under deadline pressure — a repository is *right there* and autowiring it "just this once" compiles fine until `ModularityTests` fails.
2. **A second public class with no `@NamedInterface`.** If a module needs to expose more than `<Module>Api` + `event`, that's a signal the module's public contract is growing organically instead of being designed — stop and either fold the new surface into the `Api` interface or declare a named interface for it deliberately (mirroring how `booking.event` is declared).
3. **`OPEN` module type used as an escape hatch.** `shared` is `OPEN` because it holds pure, stateless value types (`Money`, `CurrencyCode`) with zero business logic — see its own package-info comment: "if a type here starts accumulating business rules, it belongs in a real module instead." Reject any new module declared `OPEN` in review; it means someone wants to skip designing an `Api`.
4. **Cyclic dependencies introduced by a one-line import.** A module cycle (e.g., `payments` → `booking` → `payments`) is often introduced by an innocuous-looking import that compiles fine and is only caught by `ApplicationModules.verify()` — which nobody runs locally unless told to. Run `./gradlew check` (not just `test`) before pushing any change that adds a new cross-module call; don't let CI be the first place a cycle surfaces.
5. **Business logic in a controller.** `ItineraryController` today is a correct example — it injects `BookingApi` (never `BookingServiceImpl`) and has one line of logic per endpoint. Any controller method with an `if`/loop/calculation in it is doing the service layer's job in the wrong place, and it means that logic isn't reachable by anything except HTTP (no CLI job, no event listener, no test can call it directly).

### 1.3 Choosing module type

Default every module to `ENCAPSULATED`. The only currently-justified `OPEN` module is `shared`. Do not add a second `OPEN` module without discussing it here first — if you think you need one, it usually means the "shared" thing you're adding is actually a bounded context (e.g., a generic "audit log" utility is probably actually part of `compliance` or `ai` governance, not a new shared kernel type).

### 1.4 Modules exchange values, never entities

`BookingApi` never returns `Itinerary` (the JPA entity) — it returns `UUID`s and (once built out) DTOs/records. This is already the pattern; codify it: **no JPA `@Entity` class is ever a method parameter or return type on a public `Api` interface or an `event` record.** Entities are `internal/`-only by construction (package-private), so this should be structurally hard to violate — treat any counter-example as a boundary bug, not a style nit.

---

## 2. Event-Driven Design Rules

### 2.1 Direct API call vs. domain event — the decision rule

Use a **direct call through the module's `Api` interface** when:
- The caller needs a synchronous return value to proceed (e.g., the controller needs `confirmBooking`'s result before it can respond to the HTTP client).
- The caller must know about failure *before* its own transaction commits (e.g., booking confirmation should not commit if payment authorization fails).

Use a **domain event** when:
- The action is a side effect that other modules react to independently, and the originating transaction's success must not depend on the side effect succeeding. `BookingConfirmedEvent` → `notification`'s listener is the canonical example already in the repo: booking confirmation must not fail or block because a WhatsApp send is slow or down.
- Multiple modules need to react to the same fact without the originating module knowing who's listening (`payments`, `notification`, and eventually `ads` — Edge Case #11, PRD §23.5 — will all listen to booking/package-price-change events without `booking` needing to know about `ads` at all).

Rule of thumb phrased the way a reviewer should ask it: *"If this call fails, should the thing that triggered it also fail?"* Yes → direct API call. No → domain event.

### 2.2 Idempotency is mandatory, not optional

**Why:** Spring Modulith's JPA event publication registry (already wired via `spring-modulith-starter-jpa`) gives **at-least-once** delivery, not exactly-once — a listener that throws gets retried, which means it *will* be invoked more than once for the same event under real failure conditions (a crash between "listener ran" and "registry marked complete" is exactly the scenario this registry exists to survive).

Every `@ApplicationModuleListener` must be safe to run twice with the same event payload. Concretely:
- Prefer state-check-then-transition over blind mutation (`if (wallet.hasEntry(eventId)) return;` before inserting a ledger row) — natural idempotency beats infrastructure idempotency.
- Where there's no natural check, use a DB unique constraint on `(event_id, listener_name)` in a small "processed events" table and catch the constraint violation as a no-op, rather than an in-memory dedup cache (which doesn't survive the same crash the registry is designed to survive).
- Never make a listener's side effect a *pure append* without a dedup key — a "send WhatsApp notification" listener that just fires on every delivery attempt will double-notify travelers on retry, which is a trust problem for a platform whose AI governance section explicitly cares about traveler-facing trust (PRD §11.2).

**⚠️ Reconcile:** `BookingNotificationListener.on(BookingConfirmedEvent)` is currently a `// TODO` stub with no body. That's fine at this stage, but the *first* real implementation must ship with idempotency handling already in place — don't ship the "happy path" first and bolt on dedup later, because by then it's a production incident, not a design review comment.

### 2.3 Event schema versioning & evolution

Events are persisted (via the publication registry outbox) and may sit undelivered across a deploy. Treat every event record's shape as a **serialization contract with your own future deploys**, not just a Java interface:

- **Additive changes only on a published event type.** Add new fields as nullable/`Optional`, never remove or rename an existing record component. Renaming a record component changes its accessor and (depending on serialization config) its JSON/JDBC column mapping — an in-flight event serialized under the old name may fail to deserialize after deploy.
- **Breaking changes get a new event type**, not a mutated one — e.g. `BookingConfirmedEventV2`, published alongside the original for a migration window until all listeners move over, then the old type is removed in a follow-up PR once nothing publishes it anymore.
- **Enum values in an event payload are append-only.** Don't remove or renumber an enum constant referenced by a persisted event (`ItineraryStatus`, `SupplierId`, etc.) — old rows in the publication registry may still reference it.

**⚠️ Reconcile:** `BookingConfirmedEvent` currently carries `BigDecimal totalSellPrice, CurrencyCode currency` as two separate fields instead of the shared `Money` type — this is inconsistent with the Money rule in §4.4 and is exactly the kind of decomposed-money bug that rule exists to prevent (nothing stops someone reading this event from pairing the amount with the wrong currency later). Because nothing currently listens to this event for real (the notification listener is a TODO stub) and the module is pre-release, **fix this now** — change it to `record BookingConfirmedEvent(UUID bookingId, UUID consultantId, Money totalSellPrice)` — rather than carrying the inconsistency into GA and needing the V2-event migration dance in §2.3 later for a bug that was never load-bearing.

---

## 3. API Design Standards

The reference (`ItineraryController`) is deliberately thin today — `POST /api/v1/itineraries/{itineraryId}/quotation` — but it predates almost every rule below, because there was only ever one endpoint to worry about. Treat this section as the contract for every endpoint added from here forward, not a retrofit of the existing one (though it should be brought into line too — see the reconcile note).

### 3.1 REST conventions
- Resource-oriented, plural nouns, path-versioned: `/api/v1/{resource}` — already the convention (`/api/v1/itineraries`), keep it.
- Nest only one level deep for true parent/child ownership (`/itineraries/{id}/quotation` is fine because a quotation-conversion is an action *on* an itinerary). Don't nest `/consultants/{id}/itineraries/{id}/line-items/{id}/...` — flatten to `/line-items/{id}` and scope by the authenticated principal (§5.2), not by URL depth.
- Actions that don't map to a CRUD verb (e.g., "convert quotation to package," "approve AI suggestion") are `POST` to a sub-resource named as a verb-noun: `POST /quotations/{id}/conversion`, not `POST /quotations/{id}?action=convert`.

### 3.2 Versioning policy
- URL path versioning (`/api/v1`, `/api/v2`) for breaking changes only. Adding an optional field to a response, or a new optional query parameter, is **not** a breaking change and does not need a version bump.
- A breaking change (removing/renaming a field, changing a field's type or meaning, tightening validation on an existing field) requires `/api/v2/{resource}` running alongside `/api/v1` until every known consumer (today: only the frontend in this same repo) has migrated — then `v1` is removed in its own PR.

### 3.3 Error response shape

Nothing like this exists yet in the codebase (there's no `@ControllerAdvice` at all) — this is greenfield, not a retrofit, but it needs to land *before* the second and third controllers do, or every module reinvents its own error shape. Standard: **RFC 7807 Problem Details** (`application/problem+json`), extended with ADREN-specific fields:

```json
{
  "type": "https://docs.adren.travel/errors/rate-expired",
  "title": "Supplier rate expired",
  "status": 409,
  "detail": "The Hotelbeds rate for this line item expired before booking confirmation.",
  "instance": "/api/v1/itineraries/3fa8.../quotation",
  "traceId": "a1b2c3d4e5f6",
  "errors": [
    { "field": "lineItems[2].rateKey", "message": "expired" }
  ]
}
```
- `traceId` must echo the correlation ID from §6.1 so a Consultant-reported error can be grepped straight to logs.
- `errors[]` is present only for field-level validation failures (400s from `@Valid` failures); omitted otherwise.
- One `@ControllerAdvice` per module (not one global one) is acceptable and arguably preferable given module boundaries — a module's exception vocabulary (`HotelbedsClient.HotelbedsRateExpiredException` today) shouldn't require a central class in `shared` to know about every module's exception types. `shared` may define the *shape* (a `ProblemDetailFactory` helper), not the per-module `type`/`title` catalogue.

### 3.4 Pagination

Any endpoint returning a collection must be paginated — no bare `List<T>` at a controller boundary, ever, on a platform where a single Consultant can accumulate thousands of bookings over time. Use Spring Data's `Pageable`/`Page<T>` and return:
```json
{ "content": [...], "page": 0, "size": 20, "totalElements": 143, "totalPages": 8 }
```

**⚠️ Reconcile:** `BookingApi.findBookingsByConsultant` currently returns `List<UUID>`. It isn't wired to a controller yet, so this isn't user-facing today — but it must become `Page<UUID>` (or a DTO page) before it is, not after. Flag it now so nobody wires it to a controller as-is and has to do it twice.

### 3.5 Input validation

Every request body and non-trivial path/query parameter is validated via `jakarta.validation` (`@Valid` + Bean Validation annotations on a request DTO), with failures surfacing through the `errors[]` array in §3.3. `spring-boot-starter-validation` is already a dependency — it's just unused so far because there's exactly one path-variable-only endpoint.

**⚠️ Reconcile:** `ItineraryController` has zero validation today, which is currently harmless (a bare `UUID` path variable has little to validate beyond "is it a UUID," which Spring's converter already enforces). The rule matters starting with the *next* endpoint that takes a request body — don't let the "we haven't needed it yet" precedent become "we don't do it here."

---

## 4. Data & Persistence Rules

### 4.1 Entity design
- Entities live in `internal/`, package-private by default (JPA's requirement for a public no-arg constructor doesn't require the whole class to be public — `Itinerary` demonstrates this correctly). Keep it that way for every new entity.
- Business rules that govern state transitions live *on the entity* as methods (`Itinerary.markAsQuotation()` throwing `IllegalStateException` if not `DRAFT` is the right pattern — the invariant is enforced where the state lives, not scattered across service-layer `if` checks that every caller has to remember to write). Prefer this over anemic entities with public setters and validation living only in the service.
- No entity crosses a module boundary (§1.4).

### 4.2 Migration discipline (Flyway)
- One file per change, `V<n>__<description>.sql`, strictly incrementing, never edited after merge — a "fix" to a committed migration is a *new* migration, full stop. This matters more here than average because Testcontainers-backed integration tests and every developer's local Postgres both replay migration history from `V1` — an edited historical migration produces divergent checksums and silent drift between environments.
- **Enforced, not just documented (OPS-04):** `./gradlew verifyMigrationDiscipline` (part of `./gradlew check`) fails the build if any migration filename doesn't match `V<n>__description.sql`, if two files share a version number, or if `git diff` against `origin/main`/`main` shows any migration under `src/main/resources/db/migration` as modified, deleted, or renamed rather than newly added — run it locally before opening a PR, not just relying on Flyway's own runtime checksum-on-migrate check (which only fires against a database that already has the old migration applied, i.e. too late).
- `ddl-auto: validate` (already configured) stays exactly that — Hibernate never generates schema. Don't relax this "to move faster" on a feature branch; it's the thing that would otherwise let a developer's local schema and the migration history disagree silently.
- Each module owns its own tables. No shared "god table" — e.g., don't add a `payments`-owned column to the `itinerary` table because it's convenient; `payments` gets its own table and references `itinerary_id` as a foreign key value (not a foreign key *constraint* across module-owned schemas unless you're deliberately accepting that coupling — prefer application-level referential integrity for cross-module references so a module's schema can, in principle, be split into its own database later without an ALTER).
- Every migration that touches a `money`-shaped column must use `NUMERIC(19,4)` or wider (never `FLOAT`/`DOUBLE PRECISION`, never a bare `NUMERIC` without explicit precision/scale) — matches the `Money` type's `BigDecimal` discipline at the DB layer, not just the Java layer.

### 4.3 Transaction boundaries

**Why this is more than a style preference here:** the outbox pattern (Spring Modulith's JPA event publication registry) only gives you "entity state change and event publication happen atomically" if the entity save and the event publish are inside the *same* transaction. If they're not, you can persist a state change with no corresponding event (a booking silently confirmed with no notification ever queued) or publish an event for a state change that then rolls back (a notification for a booking that doesn't exist). Rule: **any service method that mutates entity state and publishes a domain event must be `@Transactional`**, and the mutation + `publishEvent` call must both happen inside that one method's transactional scope — not split across a mutate-then-separately-publish sequence in the controller.

**⚠️ Reconcile:** `BookingServiceImpl` has **no `@Transactional` annotations anywhere** today. `saveAsQuotation` mutates `Itinerary` and doesn't publish an event synchronously in the same call in a way that's visibly protected by a declared boundary — this works today only because there's a single repository save per call and no explicit event-then-rollback race has been exercised yet. This is the single highest-priority reconciliation item in this document: add `@Transactional` to `saveAsQuotation` and `confirmBooking` in `BookingServiceImpl` before either method does more than one write, and treat "service method with a repository write and no `@Transactional`" as a review-blocking finding from here forward.

### 4.4 The Money rule

**Why:** six settlement currencies, markup stacked on currency-buffer stacked on commission (PRD §12.1's worked examples) — floating point rounding error compounds specifically in multi-step percentage math, and a rounding bug here isn't cosmetic, it's a wrong invoice.

- Never use `double`/`float` for a monetary value anywhere — not in Java, not in a DTO, not in a database column, not in a frontend calculation performed before display. `Money.java`'s own Javadoc already states this; treat it as project law, not module-local convention.
- Every monetary value is the shared `Money` record (`BigDecimal amount` + `CurrencyCode currency`, scale locked to 2, `HALF_UP`), not a bare `BigDecimal`. If you find yourself writing a method that takes `BigDecimal amount, CurrencyCode currency` as two separate parameters, that's the same anti-pattern flagged in §2.3's `BookingConfirmedEvent` reconciliation — collapse it to one `Money` parameter.
- Currency mismatches fail loudly (`Money.plus` already throws `IllegalArgumentException` on mismatched currencies) — never silently coerce or ignore a currency mismatch; per PRD §23.1 Edge Case #2, a mixed-currency itinerary must be *explicitly* consolidated via the FX layer at checkout, never silently summed.
- FX rate snapshots (PRD §20.2 `fx_rate_snapshot`) are immutable once written — a refund calculated after cancellation must reuse the original snapshot (PRD §23.4 Edge Case #9), never the current rate. Model this as a value that's set once at quote time and never updated, not a value that's "usually" the snapshot but re-fetched under some code paths.

---

## 5. Security Rules

**Current state, stated plainly (updated as of FND-01):** stateless JWT authentication is now real — `security.internal.SecurityConfig` wires a `SecurityFilterChain` (`@EnableWebSecurity`/`@EnableMethodSecurity`, stateless session policy) and `JwtAuthenticationFilter` parses a `Bearer` token into an `AdrenPrincipal` (userId/role/consultantId) via `JwtTokenService`; every endpoint except `/actuator/health`/`/actuator/info` requires it (401 via `RestAuthenticationEntryPoint` otherwise), matching this story's acceptance criteria. What's still open, tracked as follow-on stories rather than silently assumed done: method-level `@PreAuthorize` expressions enforcing the PRD §6 role matrix on `Api` interfaces (`FND-02`), and the tenant-isolation check on itinerary/booking lookups (`FND-03`) — a valid JWT today proves *who* the caller is, not yet *what* they're allowed to touch. Don't let a second/third controller ship against real Consultant/traveler data before FND-02/FND-03 land.

### 5.1 AuthN/AuthZ per the role matrix (PRD §6)

- Adopt Spring Security with a stateless model (JWT bearer, or session+CSRF if server-rendered pages ever enter scope — currently they don't, frontend is a separate SPA). The principal must carry: user ID, role (`SUPER_ADMIN` / `CONSULTANT` / `USER`), and `consultant_id` (the tenant the principal belongs to — `null`/absent only for `SUPER_ADMIN`).
- Enforce the PRD §6 matrix with method-level security (`@PreAuthorize`) on `Api` interface methods, not just at the controller — the `Api` interface is the module's *entire* public surface (§1.1), so securing it there means every caller (a future scheduled job, another module, a controller) inherits the check, instead of trusting every future controller author to remember it.
- Encode the matrix as data where it's genuinely data-shaped (e.g., "can User create a Package" is a per-Consultant toggle per PRD §6's "No (unless granted)" annotations), and as `@PreAuthorize` expressions where it's structurally fixed (Super Admin never onboards on behalf of itself, etc.). Don't hardcode a `switch` on role scattered through service methods — that's the same "config that should be data" problem PRD §24.7 already calls out for KYC rules, and the same discipline applies here.

### 5.2 Tenant isolation is an authorization problem, not just a query filter

**This is the most important OWASP-relevant point for this codebase specifically.** ADREN is multi-tenant B2B — every Consultant's itineraries, wallets, traveler PII, and BYOS credentials must be invisible to every other Consultant. A UUID primary key is *not* an access control mechanism — it's just a key that's hard to guess, which is not the same as a key that's checked. Broken Object Level Authorization (OWASP API1:2023) is the realistic top risk here, concretely:

- `ItineraryController.saveAsQuotation(@PathVariable UUID itineraryId)` today has no check that the authenticated principal's `consultant_id` matches the itinerary's `consultant_id` — anyone who can reach the endpoint and guess or observe a UUID (e.g., from a shared quotation link, a browser history entry, a support ticket screenshot) can act on another Consultant's itinerary. This must be closed as part of standing up auth (§5.1), not treated as a separate hardening pass — a service method that takes an ID and doesn't verify tenant ownership against the caller's principal is incomplete, not "working but insecure."
- Enforce this at the service layer (inside `internal/`, where the repository query happens), not only at the controller — e.g., `findByConsultantId` should be called with the *authenticated* `consultant_id`, never a client-supplied one, for any endpoint reachable by `CONSULTANT`/`USER` roles. `SUPER_ADMIN`'s "view all" access is the one deliberate exception, and it should be its own explicitly-`@PreAuthorize`'d code path, not the default with role checks bolted on top.

### 5.3 Secrets handling — Consultant/BYOS/Meta credentials

- Local dev's `application.yml` credentials (`adren`/`adren` DB user, `${GROQ_API_KEY:}` env-var default) are fine *for local Docker Compose only*. No credential belonging to a real integration (Hotelbeds/STUBA/TBO API keys, a Consultant's BYOS credentials, Meta Business Manager tokens) may ever be a plaintext config value, committed file, or environment variable in a non-local environment.
- `aws-secretsmanager` is already a build dependency — use it. Adren-owned supplier credentials (Hotelbeds, STUBA, etc.) are Secrets Manager entries referenced by ARN from config, rotated per Secrets Manager's own rotation Lambda pattern rather than manually.
- BYOS credentials (Consultant-provided, per PRD §10.4/10.9) are **row-level, per-Consultant secrets** — they cannot live in a shared Secrets Manager entry keyed by supplier name the way Adren's own credentials can, because two different Consultants' BYOS Hotelbeds credentials must never be reachable through the same lookup key. Store them encrypted at the row level (KMS envelope encryption, ciphertext in Postgres, data key wrapped by a per-environment KMS CMK) and scope every read through the same tenant-isolation check as §5.2 — a BYOS credential read is exactly the kind of access an IDOR bug would expose, and the blast radius (another Consultant's live supplier account) is worse than an itinerary leak.
- Meta ad account credentials/tokens (PRD §14) are Adren-managed on Consultants' behalf, which makes them *higher*-value secrets, not lower — a leaked Meta token affects ad spend/billing liability for real money (PRD §7's "Risks" already flags "Ad account liability" as a named risk). Treat them with the same Secrets Manager + rotation discipline as Adren's own supplier credentials, never the row-level BYOS pattern (they're not Consultant-supplied).
- Never log a credential, token, or full card/payment identifier — see §6.2's PII/secret redaction rule.

### 5.4 OWASP-relevant concerns specific to this platform

- **Mass assignment.** The moment a request DTO gains more than one or two fields (imminent — see §3.5), never bind JSON directly onto a JPA `@Entity`. Bind onto a request DTO and map explicit fields onto the entity via its business constructor/methods (`Itinerary`'s package-private business constructor is already the right shape to extend this way) — this prevents a client from setting `status=BOOKED` or `ai_generated=true` by including extra JSON fields the endpoint author didn't anticipate.
- **CORS for white-label domains.** Every Consultant gets a CNAME'd custom domain (PRD §13.2). CORS configuration must be a dynamic per-Consultant allow-list resolved from the `whitelabel` module's domain registry, never a wildcard (`*`) or a static list — a wildcard CORS policy on a booking/payment API is a direct path to cross-tenant credential/session theft.
- **SSRF via Consultant-configured inputs.** Any place a Consultant supplies a URL/domain that the backend later fetches or calls back to (webhook URLs if ever added, whitelabel domain verification, BYOS base-URL overrides if that's ever supported beyond credential swapping) must validate against SSRF — no fetching of internal/link-local addresses (`169.254.169.254` matters specifically given AWS infra), and prefer an allow-list of expected external hosts over a deny-list of forbidden ones.
- **Rate limiting on search/booking endpoints.** Search aggregates and re-exposes supplier net rates (competitively sensitive data both for Adren and for suppliers under contract). An unthrottled search endpoint is a scraping vector for a competitor to reconstruct Adren's supplier pricing. This is distinct from the *per-supplier* rate limiting in §24.2/`backend-best-practices` (which protects against overrunning supplier contracts) — this is inbound rate limiting protecting Adren's own data.
- **PCI scope.** Already correctly bounded by relying on Stripe's hosted elements (PRD §24.4) — keep raw card data out of the backend entirely; don't let a "quick fix" ever introduce a raw PAN field to a DTO or log line.

---

## 6. Observability Rules

### 6.1 Correlation IDs across the event-driven flow

**Why this needs explicit design, not just "add a filter":** `@ApplicationModuleListener` is documented (and confirmed in the codebase) as `@Async @TransactionalEventListener(phase = AFTER_COMMIT)` — meaning the listener runs on a *different thread* than the request that triggered it, after the originating transaction has already committed. Thread-local correlation context (the usual MDC-based approach) does **not** cross that async boundary by default. If this isn't addressed deliberately, every log line inside `BookingNotificationListener` (and every future listener) will be uncorrelated with the request that triggered it — which defeats the purpose of a correlation ID on a platform whose core flow is explicitly event-driven.

- Generate a correlation ID (`traceId`) at the edge (a servlet filter, before Spring Security if/when added) — reuse it as the `traceId` returned in error responses (§3.3).
- Propagate it into the async event listener boundary explicitly: use Micrometer's `io.micrometer:context-propagation` (compatible with `spring-modulith-observability`, already a dependency) with a `TaskDecorator` registered on the executor backing `@Async`, so MDC context follows the event across the thread hop. Don't assume `spring-modulith-observability` gives you this for free just because it's on the classpath — verify it with a test that asserts the same `traceId` appears in a request log line and its resulting async listener's log line.
- The correlation ID must also survive into any downstream call to a supplier API (as a custom header, e.g. `X-Adren-Trace-Id`, logged on both sides) so a "Hotelbeds is timing out" incident can be traced from the Consultant's error message back to the exact outbound call.

**⚠️ Reconcile:** zero tracing/correlation configuration exists today beyond the `spring-modulith-observability` dependency being present on the classpath — no filter, no context-propagation wiring, no exporter (Zipkin/OTel) configured in `application.yml`. This needs to land before the second real `@ApplicationModuleListener` implementation (the notification listener's real body), or that feature ships un-traceable from day one.

### 6.2 Structured logging standards

- JSON structured logs (Logback's structured encoder or equivalent), not plain-text — required for anything beyond local `bootRun` console output, since six-jurisdiction support means log aggregation/search is the primary debugging tool, not tailing a file.
- Mandatory MDC fields on every log line inside a request or event-listener scope: `traceId` (§6.1), `consultantId` (the tenant), and — wherever the code path is currency- or jurisdiction-sensitive — `currency` and `market`. A "wallet debit failed" log line with no `currency` field attached is close to useless on a platform with six settlement currencies; a compliance-relevant log line (GST/TCS calculation, ATOL enforcement) with no `market` field is close to useless given per-market rule divergence (PRD §17, §24.7).
- Never log a bare monetary amount without its currency alongside it in the same structured field set — this is the logging-layer expression of the Money rule (§4.4): a number without a currency code is not auditable.
- Never log secrets, tokens, or full credential values (§5.3) — mask BYOS/supplier credential fields explicitly in any log statement or exception message that might include a request/response body (a common leak vector is logging a raw HTTP request for debugging a supplier integration issue and forgetting the auth header is in it).

### 6.3 What must be traced given AI governance + six jurisdictions

- **AI governance (PRD §11.2, §24.3) requires 100% audit logging, not sampled logging** — this is a durable, queryable, insert-only audit record (a dedicated `ai_suggestion_audit_log` table owned by the `ai` module), distinct from application logs, which may be sampled, rotated, or shipped to a log aggregator with retention limits. Don't conflate "we log AI calls" (observability) with "we have an audit trail" (compliance/trust requirement) — they have different retention, immutability, and query requirements, and the PRD is explicit that this one can't be sampled.
- **Compliance-relevant state transitions must be traceable independent of the app-log retention window** — GST/TCS calculation inputs/outputs, ATOL disclosure completion, KYC checklist state changes per market (PRD §22.9, §23.6). These belong in the `compliance` module's own persisted audit trail, not solely in ephemeral logs, for the same reason as AI governance: a tax authority or regulator asking "what did the system calculate and why" six months later cannot be answered from a log aggregator with a 30-day retention policy.
- **Every supplier call gets a distinct trace span** per §24.2's per-supplier circuit-breaker isolation requirement (see `backend-best-practices` for the resilience pattern itself) — the observability point here is that a single search request fanning out to Hotelbeds/STUBA/TBO/Mystifly/Transferz/Widgety/HBActivities in parallel needs each supplier's latency/error visible *individually* in traces, not collapsed into one "search" span, or a single slow supplier is invisible in aggregate latency metrics.

---

## 7. Frontend Architecture Rules

### 7.1 State management boundaries — React Query vs. Zustand vs. local state

Three tools are already installed (`@tanstack/react-query`, `zustand`, plus plain React state) but the codebase currently only exercises the third. Establish the boundary now, before ad hoc usage creates inconsistency across features:

| State kind | Tool | Example |
|---|---|---|
| Server data (anything fetched from `/api/v1/...`) | **React Query** — `useQuery`/`useMutation` | Search results, itinerary line items, wallet balance |
| Cross-cutting client state that outlives one component tree and isn't server data | **Zustand** | In-progress itinerary-builder draft spanning multiple wizard steps; white-label theme/branding context; toast/notification queue |
| Ephemeral, single-component-subtree UI state | **Local `useState`/`useReducer`** | A form input's draft value, a modal's open/closed flag, `SearchDashboard`'s `locationInput` today |

The rule that matters most: **React Query is the single source of truth for server-derived data.** Never copy a React Query result into Zustand or component state "to hold onto it" — that creates two caches that can silently diverge (the classic bug is: Zustand-copied data goes stale after a mutation invalidates the React Query cache, but nothing re-syncs the copy). If a piece of server data needs to be read from multiple components, that's what React Query's cache + `useQuery` in each consuming component (sharing a query key) is for — it's already deduped and cached, no manual plumbing needed.

**⚠️ Reconcile:** `useMultiLocationSearch` currently manages its own `status`/`results`/`errorMessage` state via `useState` with a mocked, synchronous `search` function — there's an explicit `// TODO: replace with a real apiClient.post('/search', ...) call`. That's appropriate for a scaffold with no real endpoint yet, but when that TODO is resolved, the replacement should be a `useMutation` (search is triggered by user action, not automatically re-fetched) or `useQuery` with a manual `enabled`/`refetch` gate — not "swap `fetch` into the existing `useState` machine and keep going." The current `SearchStatus` enum's idle/loading/success/error shape maps directly onto React Query's own `status`, so this is a rewrite of the hook's internals, not its consumers — `SearchDashboard.tsx` shouldn't need to change when this lands, which is a useful acceptance check for the migration PR.

Also **⚠️ Reconcile:** `zustand` is an installed dependency with zero usage anywhere in the codebase. That's not a problem today, but the first feature that needs cross-component client state (most likely `itinerary-builder`, which is currently an empty directory) should be the one that establishes the pattern deliberately, in a PR that's reviewed against this section — not the first of several ad hoc, inconsistent Zustand stores added under time pressure.

### 7.2 Component composition standards

- Split orchestration from presentation: a feature's root component (`SearchDashboard.tsx`-equivalent) owns data-fetching/state wiring and composes small presentational children; presentational children take typed props and contain no `useQuery`/`useMutation`/Zustand calls of their own. This isn't fully exercised yet (`SearchDashboard` is currently a single component with no extracted children) because the feature is small — but as `itinerary-builder` and later screens grow past a single-file component, extract presentational pieces rather than growing one file, and don't reach for a state-management tool inside a purely presentational component just because it's convenient.
- **Any prop object or exported hook return value with more than one field gets a named `interface`/`type`**, not an inferred shape. `LocationResult` in the search-dashboard feature already does this correctly (an exported, documented interface tying fields back to PRD sections) — keep doing that for every new API-shaped type; don't let a hook's return type stay implicit past the point where more than one field is being returned, since that's exactly when consumers benefit from a named, autocomplete-able contract.
- Every screen implements **all five states from PRD Part 21** explicitly — default, loading, results/success, empty, error — not just the happy path. `SearchDashboard` already does this (explicit `role="status"`/`role="alert"`/empty "No inventory available" branch per location) — treat that as the template, and treat a new feature PR that only implements success+error as incomplete against Part 21, not as a "we'll add loading/empty state later" follow-up.

### 7.3 Accessibility baseline

The existing `SearchDashboard` implementation is a genuinely good starting template: zero CSS framework dependency, purely semantic HTML, `<label htmlFor>` on every input, `role="status"` for the loading region and `role="alert"` for the error region (both ARIA live-region-equivalent patterns so screen readers announce async state changes without a page reload), `aria-label` on the results list. Codify this as the baseline for every new interactive component:
- Every form input has an associated `<label>` (via `htmlFor`/`id`, not placeholder-as-label).
- Every async state transition (loading → success/error) is announced via an ARIA live region (`role="status"` for non-urgent updates, `role="alert"` for errors), not conveyed by visual change alone.
- Keyboard operability is a requirement, not a nice-to-have, for every interactive element added — this is a B2B SaaS tool consultants will use for hours a day; keyboard-inaccessible controls are a productivity tax on your actual paying customers, not just a compliance checkbox.

**⚠️ Reconcile — this is the biggest concrete gap in the frontend today:** there is **no ESLint configuration file anywhere in the repo** (`.eslintrc*`/flat-config both absent) despite `eslint` and its plugins being installed and a `lint` script defined — `npm run lint` would fail outright right now. This must be fixed before the accessibility baseline above can be *enforced* rather than just hoped for. Add a flat `eslint.config.js` including `eslint-plugin-jsx-a11y` (not currently installed — add it) alongside the already-installed `eslint-plugin-react-hooks`/`eslint-plugin-react-refresh`. Given six jurisdictions and a B2B tool used professionally for hours a day, accessibility linting belongs in CI from the first real feature PR, not retrofitted once violations have accumulated across a dozen components.

### 7.4 Error boundary strategy

No `ErrorBoundary` component exists anywhere in the codebase, and `main.tsx`'s provider stack (`QueryClientProvider` → `BrowserRouter` → `App`) has no boundary wrapping it. Required structure going forward:
- **One root-level boundary** wrapping the router in `main.tsx`, rendering a generic "something went wrong, reload" fallback — the last line of defense against a fully blank white screen.
- **One boundary per top-level route/feature** (wrapping each `<Route element={...}>`), so one feature crashing (e.g., a bug in the not-yet-built `itinerary-builder`) doesn't take down navigation or an unrelated in-progress booking flow elsewhere in the app — a Consultant mid-payment on one tab-equivalent shouldn't lose that state because a different feature threw.
- Pair every boundary with React Query's `useQueryErrorResetBoundary` where the failure originated from a query, so "Retry" inside the fallback actually re-attempts the failed fetch instead of just re-rendering the same broken state.

### 7.5 Path alias — pick one convention

**Resolved.** `vite.config.ts` now has a matching `resolve.alias` for `tsconfig.json`'s `"@/*": ["src/*"]` — the design-system work (doc/DESIGN.md) adopted `@/shared/...`-style imports for cross-feature imports going forward; relative imports remain fine *within* a feature folder (e.g. `./useMultiLocationSearch`). Don't reintroduce the half-configured state by adding a second alias convention.

---

## 8. PR / Code Review Checklist

Copy the relevant block into a PR description, or use this as a reviewer's pass/fail list. Not every item applies to every PR — say so explicitly rather than silently skipping.

**Automated (OPS-05):** `.github/workflows/ci.yml` runs `./gradlew check` (backend — unit tests, `integrationTest`, `ModularityTests`, OPS-04's `verifyMigrationDiscipline`) and `npm run test:coverage` + `npm run lint` (frontend) on every PR. This makes the checks run and report status; it does **not** by itself block a merge on a red check — that additionally needs a one-time GitHub branch-protection rule (Settings → Branches → require status checks: `backend`, `frontend`) that only a repo admin can set, not something a workflow file can express.

**Fixed (TST-01), not just documented:** OPS-05 flagged pre-existing `integrationTest` flakiness under full-suite parallel execution (several `@ApplicationModuleTest`/`*IT` classes intermittently failing with Postgres connection errors) as a known, deliberately-unfixed gap. Root-caused and fixed: (1) 17 `@ApplicationModuleTest` classes each open their own Spring context (own HikariCP pool, default 10 connections) against a shared Postgres — with `junit.jupiter.execution.parallel.enabled=true`, that's up to 170/110 concurrent connections against a default `max_connections=100`. Fixed via headroom, not pool-shrinking (`docker-compose.yml`/`TestInfrastructure` now set `max_connections=200`) — an earlier attempt at shrinking the per-context pool size instead broke `BookingConcurrentConfirmationIT`/`WalletLedgerConcurrentWriteIT`, which deliberately race 8 concurrent connections from one context to test locking behavior. (2) A genuine, separate bug in `ByosCredentialCrossTenantIT`: its assertion assumed no Adren-owned Hotelbeds credential ever exists in the shared TestInfrastructure Postgres, which broke whenever `SupplierSecretsManagerIT` (which provisions exactly that) happened to run first — fixed to assert the test's actual named invariant (no cross-tenant BYOS leak), not an incidental one. Verified stable across 4 consecutive full `integrationTest` runs after both fixes.

**Module boundaries (backend)**
- [ ] No import reaches into another module's `.internal` package.
- [ ] Any new public class beyond `<Module>Api`/`event` is deliberate and named-interfaced, not accidental surface growth.
- [ ] `./gradlew check` run locally (not just `test`) — `ModularityTests.verify()` passes.
- [ ] No new `OPEN`-type module without a discussion linked in the PR.

**Events**
- [ ] New event vs. direct call decision matches §2.1's rule and is stated in the PR description if non-obvious.
- [ ] New/changed `@ApplicationModuleListener` is idempotent (state-check-then-transition, or a dedup key) — not "works once."
- [ ] Event schema change is additive, or is a new versioned event type — never an in-place rename/removal on a published event.

**API**
- [ ] New endpoint is versioned/path-conventioned per §3.1–3.2.
- [ ] Error responses use the RFC 7807 shape with `traceId` and (if applicable) `errors[]`.
- [ ] New collection endpoint is paginated — no bare `List<T>` returned from a controller.
- [ ] Request body/params validated via `@Valid`/Bean Validation.

**Data**
- [ ] New/changed entity stays in `internal/`, package-private where possible.
- [ ] New Flyway migration is additive-numbered, never edits a merged migration.
- [ ] Any service method that mutates + publishes an event is `@Transactional`.
- [ ] No `double`/`float` for money anywhere; monetary values are `Money`, never a bare `BigDecimal` + separate currency field.

**Security**
- [ ] New/changed endpoint has an explicit `@PreAuthorize` (or equivalent) matching the PRD §6 role matrix — not "no annotation means public."
- [ ] Any lookup by ID scopes to the authenticated principal's `consultant_id` — no client-supplied tenant ID trusted for authorization.
- [ ] No secret/credential/token in a config default, log line, or committed file outside local Docker Compose.
- [ ] Request DTOs used for binding, never raw entities (no mass-assignment surface).

**Observability**
- [ ] `traceId` present and propagated across any new async/event boundary.
- [ ] New log statements touching money include the currency; touching compliance/AI include `market`/audit-trail linkage.
- [ ] No secret/PII logged in full.

**Frontend**
- [ ] Server data goes through React Query; Zustand only for genuinely cross-cutting client state; local `useState` only for single-subtree ephemeral state.
- [ ] All five PRD Part 21 states implemented (default/loading/success/empty/error), not just happy path.
- [ ] New interactive elements are keyboard-operable with associated labels and appropriate ARIA live regions for async state.
- [ ] New top-level route/feature is wrapped in an error boundary.
- [ ] Exported hook/component prop shapes with >1 field have a named type.

---

## 9. Known reconciliation backlog (summary)

Pulled together from the ⚠️ boxes above, roughly in priority order:

1. Add `@Transactional` to `BookingServiceImpl.saveAsQuotation`/`confirmBooking` (§4.3) — outbox atomicity is currently unguaranteed.
2. ~~Stand up Spring Security~~ **Done (FND-01)** — stateless JWT authN is real. Remaining: method-level `@PreAuthorize` role-matrix enforcement (`FND-02`) and tenant-scoped authorization on lookups (`FND-03`) before a second real endpoint ships against real Consultant/traveler data (§5.1–5.2).
3. Fix `BookingConfirmedEvent` to carry `Money` instead of decomposed `BigDecimal`+`CurrencyCode` (§2.3) — cheap now, expensive after a real listener depends on the current shape.
4. Add correlation-ID context propagation across the `@ApplicationModuleListener` async boundary before the notification listener's real body ships (§6.1).
5. Add an `eslint.config.js` with `jsx-a11y` — `npm run lint` currently has nothing to run (§7.3).
6. Add a root + per-route `ErrorBoundary` (§7.4) — none exists.
7. ~~Resolve the `@/*` path-alias half-configuration one way or the other (§7.5).~~ Done — `resolve.alias` wired in `vite.config.ts`.
8. Convert `findBookingsByConsultant` to `Page<UUID>` before it's wired to a controller (§3.4).
9. Add circuit breakers to `SupplierAggregationService` (Resilience4j is not yet a dependency) — see `backend-best-practices` for the pattern; tracked here because it's also an NFR (PRD §24.2) the current bare try/catch doesn't satisfy.
