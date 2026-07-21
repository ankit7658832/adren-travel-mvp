plugins {
    java
    id("org.springframework.boot") version "4.1.0"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.adren"
version = "0.1.0-SNAPSHOT"
description = "ADREN TRAVEL — B2B Travel Booking Platform (MVP backend)"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

extra["springModulithVersion"] = "2.0.7"
extra["testcontainersVersion"] = "1.21.2"

dependencyManagement {
    imports {
        mavenBom("org.springframework.modulith:spring-modulith-bom:${property("springModulithVersion")}")
        mavenBom("org.testcontainers:testcontainers-bom:${property("testcontainersVersion")}")
    }
}

dependencies {
    // --- Core ---
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // --- Security: stateless JWT auth + method-level @PreAuthorize (RULES.md §5.1) ---
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")
    testImplementation("org.springframework.security:spring-security-test")

    // --- Spring Modulith: modular monolith structure + event publication registry ---
    implementation("org.springframework.modulith:spring-modulith-starter-core")
    // JDBC-backed (not JPA-entity-backed) event publication registry: its
    // schema is a plain SQL table Flyway owns (V3__init_modulith_event_publication.sql),
    // matching RULES.md §4.2's "ddl-auto: validate, never generate schema"
    // rule — the JPA variant has no bundled schema script and only works
    // with a relaxed ddl-auto, which this project deliberately never allows.
    implementation("org.springframework.modulith:spring-modulith-starter-jdbc") // event publication log (outbox-style)
    implementation("org.springframework.modulith:spring-modulith-events-api")
    implementation("org.springframework.modulith:spring-modulith-actuator")
    implementation("org.springframework.modulith:spring-modulith-observability")

    // --- AWS integration (LocalStack-backed: S3 for vouchers, SQS/SNS for event fan-out) ---
    implementation(platform("software.amazon.awssdk:bom:2.29.29"))
    implementation("software.amazon.awssdk:s3")
    implementation("software.amazon.awssdk:sqs")
    implementation("software.amazon.awssdk:sns")
    implementation("software.amazon.awssdk:secretsmanager")
    // FND-12 — BYOS credential envelope encryption (KMS wraps the AES data
    // key; the actual credential ciphertext is encrypted locally, never
    // sent to KMS itself).
    implementation("software.amazon.awssdk:kms")

    // --- Persistence ---
    runtimeOnly("org.postgresql:postgresql")
    // Spring Boot 4 split Flyway's Spring auto-configuration glue out of the
    // monolithic spring-boot-autoconfigure jar into its own module (same
    // restructuring as spring-boot-hibernate, spring-boot-jdbc, etc.) — the
    // raw org.flywaydb:flyway-core/-database-postgresql libraries alone are
    // NOT enough to make Spring actually run migrations on startup anymore.
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.flywaydb:flyway-database-postgresql")

    // --- Lombok (optional, remove if the team prefers records/plain POJOs) ---
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // --- Groq (free-tier LLM) client for the AI Governance module (Section 11) ---
    // Groq exposes an OpenAI-compatible REST API — plain WebClient is sufficient,
    // no dedicated SDK dependency required.
    implementation("org.springframework.boot:spring-boot-starter-webflux")

    // --- Resilience4j: per-supplier circuit breaker (PRD §24.2, BOK-26) ---
    // Programmatic CircuitBreakerRegistry usage only (SupplierCircuitBreakerGateway) —
    // the plain circuitbreaker module is enough, no Spring AOP/annotation
    // wiring (resilience4j-spring-boot3) needed for this.
    implementation("io.github.resilience4j:resilience4j-circuitbreaker:2.2.0")

    // --- CSV parsing for DMC-03's Local DMC bulk-inventory-upload tool ---
    // Real business text fields (cancellation policy text) can genuinely
    // contain commas, so a hand-rolled comma-split parser would silently
    // misparse rows — commons-csv is a small, dependency-free, well-tested
    // RFC4180 parser, not a heavier "spreadsheet" library.
    implementation("org.apache.commons:commons-csv:1.12.0")

    // --- Test ---
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    // Spring Boot 4 split web MVC's test-slice support (@WebMvcTest) out of
    // the monolithic spring-boot-test-autoconfigure jar, same restructuring
    // as the Flyway split above — spring-boot-starter-test alone no longer
    // pulls it in since not every project uses Spring MVC.
    testImplementation("org.springframework.boot:spring-boot-webmvc-test")
    testImplementation("org.mockito:mockito-junit-jupiter")
    testImplementation("org.springframework.modulith:spring-modulith-starter-test")
    testImplementation("org.springframework.modulith:spring-modulith-docs")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:localstack")
    testImplementation("io.rest-assured:rest-assured:5.5.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
    // Keep unit and integration tests separable in CI:
    //   ./gradlew test              -> fast unit tests (see testing-strategy skill)
    //   ./gradlew integrationTest   -> Testcontainers/LocalStack-backed tests
    systemProperty("junit.jupiter.execution.parallel.enabled", "true")

    // Testcontainers compatibility defaults for non-Docker-Desktop backends
    // (Rancher Desktop, Colima, Lima, Podman) — see backend/README.md's
    // "Testcontainers on non-Docker-Desktop backends" section for the two
    // failure modes these work around. Both are no-ops/harmless on a
    // standard Docker Desktop or CI Docker-in-Docker setup, and both remain
    // overridable (-Dapi.version=... on the gradlew invocation, or an
    // actual TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE env var already set).
    systemProperty("api.version", System.getProperty("api.version") ?: "1.41")
    environment(
        "TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE",
        System.getenv("TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE") ?: "/var/run/docker.sock"
    )

    // TST-01 — root cause of the parallel-integrationTest flakiness flagged
    // in OPS-05/RULES.md S8: 17 @ApplicationModuleTest classes each open
    // their own Spring context (own HikariCP pool, default max-pool-size
    // 10) against the SAME shared Postgres (the ambient docker-compose one
    // for module-slice tests, TestInfrastructure's ephemeral one for *IT
    // classes) — with junit.jupiter.execution.parallel.enabled=true firing
    // several of those contexts at once, that's up to 170/110 concurrent
    // connections against a default max_connections=100. Fixed via headroom
    // (docker-compose.yml / TestInfrastructure raise max_connections to
    // 200), NOT by shrinking the per-context pool size — an earlier attempt
    // at that broke BookingConcurrentConfirmationIT/WalletLedgerConcurrentWriteIT,
    // which deliberately race 8 concurrent connections from a single
    // context to test locking behavior and need real headroom within one
    // context, not just in aggregate.

    // OPS-06 — JDK 25 tightened dynamic Java-agent loading (Mockito's
    // inline-mock-maker self-attaches at runtime rather than being
    // declared as a build-time -javaagent). Without this, every test run
    // using Mockito.mock(...) prints "Mockito is currently self-attaching
    // ... this will no longer work in future releases of the JDK" —
    // explicitly opting in is the fix the warning itself suggests, not a
    // long-term-viable thing to silently ignore.
    jvmArgs("-XX:+EnableDynamicAgentLoading")
}

// Separate source set + task for integration tests so `./gradlew test` stays fast
// and CI can run unit and integration suites as distinct pipeline stages.
sourceSets {
    create("integrationTest") {
        java.srcDir("src/integrationTest/java")
        resources.srcDir("src/integrationTest/resources")
        compileClasspath += sourceSets.main.get().output + sourceSets.test.get().output
        runtimeClasspath += sourceSets.main.get().output + sourceSets.test.get().output
    }
}

configurations.named("integrationTestImplementation") {
    extendsFrom(configurations.testImplementation.get())
}
configurations.named("integrationTestRuntimeOnly") {
    extendsFrom(configurations.testRuntimeOnly.get())
}

tasks.register<Test>("integrationTest") {
    description = "Runs Testcontainers/LocalStack-backed integration and Spring Modulith module tests."
    group = "verification"
    testClassesDirs = sourceSets["integrationTest"].output.classesDirs
    classpath = sourceSets["integrationTest"].runtimeClasspath
    useJUnitPlatform()
    shouldRunAfter(tasks.test)
}

// TST-06, PRD S23.2 Edge Case #4 / S25 T19-T20 — sandbox and production
// supplier environments are documented to behave differently (Hotelbeds/TBO
// specifically). Real production fixtures are stubbed/synthetic in MVP
// (real production access is Phase 2's SUP-* epic), so these two tasks
// separate the ALWAYS-SUCCEEDS supplier-client tests (@Tag
// "supplier-sandbox-fixture") from the documented-quirk ones (@Tag
// "supplier-production-fixture" — TBO TraceId expiry mid-build, Mystifly-
// shaped stale-fare detection) across BOTH the test and integrationTest
// source sets, reported as two distinct, separately-flagged CI jobs rather
// than assumed equivalent.
val supplierFixtureTestClasspath = sourceSets["test"].runtimeClasspath + sourceSets["integrationTest"].runtimeClasspath
val supplierFixtureTestClassesDirs = sourceSets["test"].output.classesDirs + sourceSets["integrationTest"].output.classesDirs

tasks.register<Test>("supplierSandboxFixtureTests") {
    description = "TST-06: supplier client tests against sandbox-shaped fixtures (stable session/fare, no expiry)."
    group = "verification"
    testClassesDirs = supplierFixtureTestClassesDirs
    classpath = supplierFixtureTestClasspath
    useJUnitPlatform {
        includeTags("supplier-sandbox-fixture")
    }
    shouldRunAfter(tasks.test, tasks.named("integrationTest"))
}

tasks.register<Test>("supplierProductionFixtureTests") {
    description = "TST-06: supplier client tests against production-shaped fixtures (TBO TraceId expiry T19, stale-fare detection T20)."
    group = "verification"
    testClassesDirs = supplierFixtureTestClassesDirs
    classpath = supplierFixtureTestClasspath
    useJUnitPlatform {
        includeTags("supplier-production-fixture")
    }
    shouldRunAfter(tasks.test, tasks.named("integrationTest"))
}

// OPS-08 — release-checklist step: ModularityTests.writeModuleDocumentation()
// (run as part of ./gradlew test, no separate wiring needed for generation
// itself) regenerates PlantUML module diagrams under build/spring-modulith-docs
// straight from the actual code structure. This task is the second half —
// copying them into doc/architecture/ — that doc/README.md's "Regenerating
// architecture diagrams" section already documented as a manual step but
// nothing ever ran. Deliberately NOT wired into `check`/CI: this is a
// release-process step a human runs and reviews the diff of, not a
// pass/fail gate on every PR.
tasks.register<Copy>("updateModuleDocs") {
    group = "documentation"
    description = "OPS-08: copies the regenerated Spring Modulith PlantUML module diagrams into doc/architecture/ — run before a release PR and review the diff."
    dependsOn(tasks.named("test"))
    from(layout.buildDirectory.dir("spring-modulith-docs"))
    into(rootDir.resolve("../doc/architecture"))
    include("*.puml")
}

// OPS-04, RULES.md S4.2 — every module's migrations must be strictly
// incrementing (V<n>__description.sql, no duplicate/out-of-order version
// numbers) and a merged migration must never be edited or deleted after
// the fact (only Flyway's own checksum-on-migrate check catches that at
// runtime, against a real applied-history DB — this catches it in CI,
// before merge, against git history instead).
tasks.register("verifyMigrationDiscipline") {
    group = "verification"
    description = "OPS-04: Flyway migration numbering is strictly incrementing and no merged migration was edited or deleted."

    val migrationDir = layout.projectDirectory.dir("src/main/resources/db/migration")
    val projectDir = layout.projectDirectory.asFile

    doLast {
        val versionRegex = Regex("""^V(\d+)__.+\.sql$""")
        val files = migrationDir.asFile.listFiles { f -> f.isFile }?.toList() ?: emptyList()
        val versioned = files.map { f ->
            val match = versionRegex.matchEntire(f.name)
                ?: throw GradleException("OPS-04: migration file '${f.name}' doesn't match V<n>__description.sql")
            match.groupValues[1].toLong() to f.name
        }

        val duplicates = versioned.groupBy { it.first }.filterValues { it.size > 1 }
        if (duplicates.isNotEmpty()) {
            throw GradleException("OPS-04: duplicate Flyway migration version number(s): " +
                duplicates.entries.joinToString { (version, entries) -> "V$version -> ${entries.map { it.second }}" })
        }

        val sortedByVersion = versioned.sortedBy { it.first }
        for (i in 1 until sortedByVersion.size) {
            if (sortedByVersion[i].first <= sortedByVersion[i - 1].first) {
                throw GradleException("OPS-04: migration versions not strictly increasing at " +
                    "${sortedByVersion[i].second} (V${sortedByVersion[i].first}) after " +
                    "${sortedByVersion[i - 1].second} (V${sortedByVersion[i - 1].first})")
            }
        }

        val baseRef = listOf("origin/main", "main").firstOrNull { ref ->
            ProcessBuilder("git", "rev-parse", "--verify", ref)
                .directory(projectDir).redirectErrorStream(true).start().waitFor() == 0
        }
        if (baseRef == null) {
            logger.warn("OPS-04: no 'origin/main' or 'main' git ref found — skipping the " +
                "no-edits-to-merged-migrations check (nothing to diff against).")
        } else {
            val diffProcess = ProcessBuilder(
                "git", "diff", "--name-status", baseRef, "--", "src/main/resources/db/migration"
            ).directory(projectDir).redirectErrorStream(true).start()
            val diffOutput = diffProcess.inputStream.bufferedReader().readText()
            diffProcess.waitFor()

            val violations = diffOutput.lineSequence()
                .filter { it.isNotBlank() }
                .filterNot { it.startsWith("A\t") }
                .toList()
            if (violations.isNotEmpty()) {
                throw GradleException("OPS-04: merged migration(s) edited/deleted/renamed relative to " +
                    "'$baseRef' — a merged migration must never change after the fact " +
                    "(RULES.md S4.2). Add a NEW migration instead:\n" + violations.joinToString("\n"))
            }
        }
    }
}

tasks.named("check") {
    dependsOn(tasks.named("integrationTest"))
    dependsOn(tasks.named("verifyMigrationDiscipline"))
}
