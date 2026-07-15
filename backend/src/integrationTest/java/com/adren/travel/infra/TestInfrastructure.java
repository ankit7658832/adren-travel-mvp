package com.adren.travel.infra;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.CreateKeyRequest;

import java.net.URI;

/**
 * Shared Testcontainers setup for integration tests: a real Postgres
 * instance (not H2 — PRD Section 24 NFRs assume real Postgres behavior,
 * e.g. decimal precision) and a real LocalStack instance backing
 * S3/SQS/SNS/Secrets Manager/KMS (PRD's chosen MVP substitute for real AWS,
 * per the earlier "event-driven architecture ... with the help of
 * localstack" requirement).
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
                LocalStackContainer.Service.SECRETSMANAGER,
                LocalStackContainer.Service.KMS
            );

    /**
     * FND-12 — LocalStack KMS ships no well-known key/alias the way
     * Secrets Manager needs no pre-created "directory"; a real KMS
     * {@code CreateKey} call is required once per container lifetime so
     * {@code adren.aws.kms-key-id} resolves to a real key ID.
     */
    static final String KMS_KEY_ID;

    static {
        POSTGRES.start();
        LOCALSTACK.start();
        KmsClient bootstrapKmsClient = KmsClient.builder()
            .endpointOverride(URI.create(LOCALSTACK.getEndpoint().toString()))
            .region(Region.of(LOCALSTACK.getRegion()))
            .credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create(LOCALSTACK.getAccessKey(), LOCALSTACK.getSecretKey())))
            .build();
        KMS_KEY_ID = bootstrapKmsClient.createKey(CreateKeyRequest.builder()
            .description("adren-travel-test BYOS credential envelope key")
            .build()).keyMetadata().keyId();
        bootstrapKmsClient.close();
    }

    @Override
    public void initialize(ConfigurableApplicationContext context) {
        TestPropertyValues.of(
            "spring.datasource.url=" + POSTGRES.getJdbcUrl(),
            "spring.datasource.username=" + POSTGRES.getUsername(),
            "spring.datasource.password=" + POSTGRES.getPassword(),
            "adren.aws.endpoint-override=" + LOCALSTACK.getEndpoint(),
            "adren.aws.region=" + LOCALSTACK.getRegion(),
            "adren.aws.kms-key-id=" + KMS_KEY_ID
        ).applyTo(context.getEnvironment());
    }
}
