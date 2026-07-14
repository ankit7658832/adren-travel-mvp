package com.adren.travel;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClientBuilder;

import java.net.URI;

/**
 * RULES.md §5.3 — Adren's own supplier credentials live in Secrets Manager
 * (LocalStack in dev/test), never as a plaintext config value (FND-11).
 * LocalStack doesn't validate credentials but the SDK still requires a
 * {@code AwsCredentialsProvider}, hence the static placeholder below only
 * when an endpoint override (LocalStack) is configured — a real,
 * non-local profile always uses {@link DefaultCredentialsProvider} (the
 * standard env/instance-profile/IAM-role resolution chain) against the
 * real regional endpoint.
 */
@Configuration
class AwsSecretsManagerConfig {

    @Bean
    SecretsManagerClient secretsManagerClient(AwsProperties properties) {
        SecretsManagerClientBuilder builder = SecretsManagerClient.builder()
            .region(Region.of(properties.region()));

        if (properties.hasEndpointOverride()) {
            builder.endpointOverride(URI.create(properties.endpointOverride()))
                .credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create("localstack", "localstack")));
        } else {
            builder.credentialsProvider(DefaultCredentialsProvider.create());
        }

        return builder.build();
    }
}
