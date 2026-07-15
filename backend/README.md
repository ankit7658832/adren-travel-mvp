# ADREN TRAVEL — Backend

Spring Boot 3 + Spring Modulith + Gradle (Kotlin DSL). Java 25 toolchain
target (see `build.gradle.kts` — bump once a Java 25 JDK is available in
your build environment; this scaffold was authored against a Java 21 JDK).

## Module map (matches PRD Sections 9–17)

| Package | PRD Module | Status |
|---|---|---|
| `booking` | Core Booking Engine (Section 9) | Reference implementation |
| `supplier` | Supplier & Inventory Integration (Section 10) | Reference implementation (Hotelbeds stub; STUBA/TBO/others follow the same pattern); Adren-owned credentials in Secrets Manager by ARN (FND-11) and Consultant-owned BYOS credentials as row-level KMS-envelope-encrypted secrets (FND-12) |
| `ai` | AI Itinerary & Governance (Section 11) | Package-info stub — build next |
| `payments` | Payments, Yield/Markup & Wallet (Section 12) | Package-info stub |
| `whitelabel` | White-Label & Admin Console (Section 13) | Consultant onboarding + market-driven KYC (FND-04), lifecycle suspend/reinstate (FND-05), branding config with a 30s-TTL cache + commit-time invalidation (FND-06/FND-07), locale/market selection (FND-17), and the dynamic per-Consultant CORS domain registry (FND-08) implemented |
| `ads` | Ads/Campaign Management (Section 14) | Package-info stub |
| `notification` | Notifications (Section 15) | Reference event listener implemented |
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
./gradlew test                # unit tests + @ApplicationModuleTest slices (fast, no Docker required except for Modulith's embedded test DB)
./gradlew integrationTest      # Testcontainers-backed end-to-end tests (requires Docker)
./gradlew check                # both, plus module boundary verification (ModularityTests)
```

See the `testing-strategy` Claude Code skill for the full test-tier convention.

## Generating module documentation

`ModularityTests.writeModuleDocumentation()` regenerates PlantUML module
diagrams under `build/spring-modulith-docs`. Copy the output into
`../doc/architecture/` as part of your release checklist so the module map
never drifts from the code.
