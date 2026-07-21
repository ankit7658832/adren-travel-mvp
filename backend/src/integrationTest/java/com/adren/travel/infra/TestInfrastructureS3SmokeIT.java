package com.adren.travel.infra;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TST-01 — the sample test this story's own sub-tasks call for: proves
 * {@link TestInfrastructure}'s shared LocalStack container genuinely
 * supports S3 (not just Secrets Manager/KMS, the only services any
 * existing *IT class had actually exercised before this — no module has a
 * real S3-backed {@code DocumentStorage} implementation yet, per its own
 * "production-tier work" note). Any future module wiring real S3 usage
 * (BOK-15's voucher upload, BOK-14's document vault) gets this same
 * container for free — no new container setup needed, exactly this
 * story's acceptance criterion.
 */
@SpringJUnitConfig
@ContextConfiguration(initializers = TestInfrastructure.class, classes = TestInfrastructureS3SmokeIT.EmptyConfig.class)
class TestInfrastructureS3SmokeIT {

    static class EmptyConfig {
    }

    @Test
    void aBucketCanBeCreatedAndAnObjectRoundTrippedThroughTheSharedLocalStackContainer() {
        S3Client s3Client = S3Client.builder()
            .endpointOverride(URI.create(TestInfrastructure.LOCALSTACK.getEndpoint().toString()))
            .region(Region.of(TestInfrastructure.LOCALSTACK.getRegion()))
            .credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create(TestInfrastructure.LOCALSTACK.getAccessKey(),
                    TestInfrastructure.LOCALSTACK.getSecretKey())))
            .forcePathStyle(true)
            .build();

        String bucket = "tst-01-smoke-test";
        s3Client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
        s3Client.putObject(PutObjectRequest.builder().bucket(bucket).key("hello.txt").build(),
            RequestBody.fromString("hello from TestInfrastructure's shared LocalStack"));

        String content = s3Client.getObjectAsBytes(GetObjectRequest.builder().bucket(bucket).key("hello.txt").build())
            .asUtf8String();

        assertThat(content).isEqualTo("hello from TestInfrastructure's shared LocalStack");
        s3Client.close();
    }
}
