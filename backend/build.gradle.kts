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

tasks.named("check") {
    dependsOn(tasks.named("integrationTest"))
}
