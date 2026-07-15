package com.adren.travel;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.KmsClientBuilder;

import java.net.URI;

/**
 * RULES.md §5.3 / FND-12 — the KMS client backing BYOS credential envelope
 * encryption (LocalStack in dev/test). Mirrors {@link AwsSecretsManagerConfig}'s
 * shape exactly: a static LocalStack credentials provider only when an
 * endpoint override is configured, {@link DefaultCredentialsProvider}
 * against the real regional endpoint otherwise.
 */
@Configuration
class AwsKmsConfig {

    @Bean
    KmsClient kmsClient(AwsProperties properties) {
        KmsClientBuilder builder = KmsClient.builder()
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
