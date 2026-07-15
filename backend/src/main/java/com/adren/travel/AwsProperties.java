package com.adren.travel;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds {@code adren.aws.*} from application.yml. {@code endpointOverride}
 * points at LocalStack for local/dev/test; every non-local profile must
 * leave it unset so the AWS SDK resolves the real regional endpoint
 * instead (RULES.md §5.3) — see {@link AwsSecretsManagerConfig}.
 * {@code kmsKeyId} is the customer master key {@code KmsEnvelopeEncryptionService}
 * (FND-12) wraps every BYOS credential's data key with.
 */
@ConfigurationProperties(prefix = "adren.aws")
record AwsProperties(String endpointOverride, String region, String kmsKeyId) {

    boolean hasEndpointOverride() {
        return endpointOverride != null && !endpointOverride.isBlank();
    }
}
