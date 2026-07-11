package com.adren.travel.infra;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Shared Testcontainers setup for integration tests: a real Postgres
 * instance (not H2 — PRD Section 24 NFRs assume real Postgres behavior,
 * e.g. decimal precision) and a real LocalStack instance backing S3/SQS/SNS
 * (PRD's chosen MVP substitute for real AWS, per the earlier "event-driven
 * architecture ... with the help of localstack" requirement).
 * <p>
 * Usage: annotate an integration test class with
 * {@code @ContextConfiguration(initializers = TestInfrastructure.class)}
 * or extend a base class that does so — see
 * {@code BookingRepositoryIT} for the pattern.
 */
public class TestInfrastructure implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
            .withDatabaseName("adren_travel_test")
            .withUsername("adren")
            .withPassword("adren");

    static final LocalStackContainer LOCALSTACK =
        new LocalStackContainer(DockerImageName.parse("localstack/localstack:3.8"))
            .withServices(
                LocalStackContainer.Service.S3,
                LocalStackContainer.Service.SQS,
                LocalStackContainer.Service.SNS,
                LocalStackContainer.Service.SECRETSMANAGER
            );

    static {
        POSTGRES.start();
        LOCALSTACK.start();
    }

    @Override
    public void initialize(ConfigurableApplicationContext context) {
        TestPropertyValues.of(
            "spring.datasource.url=" + POSTGRES.getJdbcUrl(),
            "spring.datasource.username=" + POSTGRES.getUsername(),
            "spring.datasource.password=" + POSTGRES.getPassword(),
            "adren.aws.endpoint-override=" + LOCALSTACK.getEndpoint(),
            "adren.aws.region=" + LOCALSTACK.getRegion()
        ).applyTo(context.getEnvironment());
    }
}
