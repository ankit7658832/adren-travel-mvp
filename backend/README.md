# ADREN TRAVEL — Backend

Spring Boot 3 + Spring Modulith + Gradle (Kotlin DSL). Java 25 toolchain
target (see `build.gradle.kts` — bump once a Java 25 JDK is available in
your build environment; this scaffold was authored against a Java 21 JDK).

## Module map (matches PRD Sections 9–17)

| Package | PRD Module | Status |
|---|---|---|
| `booking` | Core Booking Engine (Section 9) | Reference implementation; Hotel line items (BOK-03) priced through the `payments` module's FIN-05 pipeline; saving as a Quotation now requires a line item and creates a `Quotation` row with a `validUntil` window, and becomes read-only to further line items (BOK-08/BOK-09); converting a Quotation to a reusable `TravelPackage` with an auto-filled base price and editable markup, gated by PRD §6's "Create package" role/capability-grant rule (BOK-10); publishing a Package (DRAFT→PUBLISHED, visibility query, promoted-via-ads flag — BOK-12); `confirmBooking` now resolves a real `consultantId` from the given Quotation or Package instead of a random stub, with `POST /api/v1/bookings` exposing it (BOK-13 backend scaffold); its direct (non-Stripe) path places then immediately resolves a wallet hold as a debit via `payments`'s FIN-07 lifecycle (FIN-08's credit-limit breach block is a separate, not-yet-built story); Stripe-webhook-gated booking confirmation (FIN-11), which never touches the wallet (the customer paid by card); every booking confirmation generates and persists a `Voucher` (real `pdf_reference` via a stubbed `DocumentStorage` seam mirroring FIN-11's `StripeClient` pattern, actual PDF rendering and S3/LocalStack upload deferred to OPS-01/OPS-03) in the SAME transactional scope as the confirmation itself, not async (BOK-15) — `atolCertificateReference` always stays `null` in this slice, same reason as `dynamicFlightHotelCombo` below. **BOK-11 (Package Builder screen + UK ATOL disclosure gate) and BOK-13's frontend (Booking & Payment Flow screen, hooks, Playwright e2e) are deliberately deferred**: both are majority-frontend, out of this backend-only vertical slice's scope, and BOK-11's backend half — detecting a flight+hotel "dynamic combo" — has nothing to detect yet since no Flight line item type exists (BOK-04); `TravelPackage.dynamicFlightHotelCombo` stays hard-defaulted `false` until BOK-04 lands |
| `supplier` | Supplier & Inventory Integration (Section 10) | Reference implementation (Hotelbeds stub; STUBA/TBO/others follow the same pattern); Adren-owned credentials in Secrets Manager by ARN (FND-11) and Consultant-owned BYOS credentials as row-level KMS-envelope-encrypted secrets (FND-12) |
| `ai` | AI Itinerary & Governance (Section 11) | Package-info stub — build next |
| `payments` | Payments, Yield/Markup & Wallet (Section 12) | Per-Consultant, per-category markup configuration (FIN-01), wallet balance/credit-limit/pending-holds query with lazy auto-provisioning (FIN-06), Adren commission separate from Consultant markup (FIN-02), currency buffer before markup (FIN-03), locked FX rate snapshotting (FIN-04), the full net→buffer→markup→commission `PricingPipeline` (FIN-05), Stripe PaymentIntent creation + webhook-driven booking confirmation behind a stubbed `StripeClient` seam (FIN-11), and the wallet hold lifecycle — `placeHold`/`resolveHoldAsDebit`/`resolveHoldAsRelease` writing to an insert-only `WalletLedgerEntry` audit trail (FIN-07) — made idempotent under real concurrency via a `(related_booking_id, type)` unique constraint plus a `REQUIRES_NEW`-scoped `WalletLedgerEntryRecorder` that catches the constraint violation without poisoning the caller's transaction (FIN-10) implemented |
| `whitelabel` | White-Label & Admin Console (Section 13) | Consultant onboarding + market-driven KYC (FND-04), lifecycle suspend/reinstate (FND-05), branding config with a 30s-TTL cache + commit-time invalidation (FND-06/FND-07), locale/market selection (FND-17), the dynamic per-Consultant CORS domain registry (FND-08), and `findConsultantMarket` (HRD-01, no `@PreAuthorize` — consulted mid-flow by an already-authorized system step, same shape as `requireConsultantActive`) implemented |
| `ads` | Ads/Campaign Management (Section 14) | Package-info stub |
| `notification` | Notifications (Section 15) | `BookingConfirmedEvent` dispatch: email always, plus a region-routed secondary channel (WhatsApp for India/Dubai, SMS elsewhere, data-driven per RULES.md §24.7) resolved via `whitelabel.WhitelabelApi.findConsultantMarket`, with a defensive SMS fallback for any consultantId that isn't a real onboarded Consultant (HRD-01). Email/WhatsApp/SMS all sit behind stubbed seams (`StubEmailClient`/`StubWhatsAppClient`/`StubSmsClient`) mirroring FIN-11's `StripeClient` pattern — no real provider is called yet. Per-Consultant channel overrides (HRD-04) aren't built, so the market default always applies |
| `compliance` | Regional Compliance & Localization (Section 17) | Package-info stub |
| `shared` | Shared kernel (Money, CurrencyCode, PageResponse, TraceIds) | Complete |
| `security` | AuthN/AuthZ (Section 6) | Stateless JWT + method-level @PreAuthorize implemented (FND-01/FND-02) |

Each module follows the same shape:
```
<module>/
├── package-info.java       (@ApplicationModule — module boundary declaration)
├── <Module>Api.java         (public interface — the ONLY thing other modules may depend on)
├── event/                   (@NamedInterface("event") — domain events other modules may listen to)
└── internal/                (hidden from other modules — entities, repositories, service impl, controllers)
```

See the `backend-spring-modulith` Claude Code skill (`.claude/skills/`) before adding a new module or touching cross-module wiring.

## Running locally

```bash
docker compose up -d          # Postgres + LocalStack
./gradlew bootRun
```

## Testing

```bash
docker compose up -d           # required before integrationTest — see note below
./gradlew test                 # unit tests + @ApplicationModuleTest slices (fast, no Docker required except for Modulith's embedded test DB)
./gradlew integrationTest      # Testcontainers-backed end-to-end tests (requires Docker)
./gradlew check                # both, plus module boundary verification (ModularityTests)
```

See the `testing-strategy` Claude Code skill for the full test-tier convention.

**`docker compose up -d` must already be running before `./gradlew integrationTest`.** The integration-test suite has two different infrastructure-provisioning paths that both need to be up: `*IT` classes (e.g. `CreditLimitBreachIT`) use `TestInfrastructure`, which spins up its own ephemeral Testcontainers-managed Postgres/LocalStack on random ports; `@ApplicationModuleTest` module-slice classes (e.g. `SupplierModuleIntegrationTests`) have no such bootstrap and instead connect to the *ambient* fixed-port stack (`localhost:5432`/`localhost:4566`) that `docker-compose.yml` starts. Skipping `docker compose up -d` doesn't fail loudly — it surfaces as `FlywaySqlUnableToConnectToDbException`/`ConnectException` on exactly the module-slice classes, which looks like flaky parallel-execution resource contention rather than a missing prerequisite. It isn't — start the compose stack first.

### Testcontainers on non-Docker-Desktop backends (Rancher Desktop, Colima, Lima, Podman)

`./gradlew integrationTest` was, for several stages, only ever confirmed to *compile* against Testcontainers, never actually run — every environment hit a `"client version 1.32 is too old"` error and it was carried forward as a known caveat rather than root-caused (see `doc/phases.md` §7c/§7e). It's since been root-caused: there are two distinct compatibility gaps, both already fixed at the project level in `build.gradle.kts`, plus one machine-resource prerequisite that can't be fixed from a config file and has to be set locally.

1. **docker-java defaults to requesting Docker API version 1.32, which recent Docker Engine releases (min supported API 1.41+) reject outright.** This is *not* controlled by the `DOCKER_API_VERSION` environment variable, despite that being the commonly-suggested fix online — the shaded docker-java client bundled inside Testcontainers only reads the JVM system property `api.version`. Fixed: `build.gradle.kts`'s `tasks.withType<Test>` block now sets `systemProperty("api.version", "1.41")` on every test task, so this is automatic. Override with `-Dapi.version=<version>` on the `./gradlew` invocation if a future Docker Engine raises its minimum further.
2. **Testcontainers' Ryuk cleanup sidecar fails to start** (`Container startup failed for image testcontainers/ryuk:0.12.0`, root cause `error while creating mount source path '<host docker.sock path>': mkdir ...: operation not supported`) on any backend where the host-visible Docker socket path (e.g. Rancher Desktop's `~/.rd/docker.sock`) isn't the same path the socket lives at *inside* the container-runtime VM — Ryuk tries to bind-mount the host path literally. Fixed: `build.gradle.kts` sets `TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock` (the in-VM path) on every test task. This is a no-op on real Docker Desktop / standard Linux CI runners, where that's already the correct path.
3. **Not fixable from this repo: the container runtime's VM needs enough memory.** Once (1) and (2) are resolved, LocalStack (5 services: S3/SQS/SNS/Secrets Manager/KMS) running alongside Postgres and Ryuk was observed getting silently OOM-killed (`docker inspect <container> --format '{{.State.OOMKilled}}'` → `true`) on a Rancher Desktop VM sized at the ~2GB/2-CPU default (`docker info` → `MemTotal`) — LocalStack itself recommends 4GB+ for multi-service use. If `integrationTest` hangs or fails with `SdkClientException: ... target server failed to respond` / `Read timed out` talking to the LocalStack endpoint, check `docker info`'s `MemTotal` first before assuming it's a code or config regression. **Fix:** increase the Docker backend's VM memory allocation (Rancher Desktop: Preferences → Virtual Machine → Memory; Docker Desktop: Settings → Resources → Memory) to at least 4GB, ideally 6–8GB for headroom.

## Generating module documentation

`ModularityTests.writeModuleDocumentation()` regenerates PlantUML module
diagrams under `build/spring-modulith-docs`. Copy the output into
`../doc/architecture/` as part of your release checklist so the module map
never drifts from the code.
