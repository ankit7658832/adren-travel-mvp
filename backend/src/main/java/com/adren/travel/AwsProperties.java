package com.adren.travel;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds {@code adren.aws.*} from application.yml. {@code endpointOverride}
 * points at LocalStack for local/dev/test; every non-local profile must
 * leave it unset so the AWS SDK resolves the real regional endpoint
 * instead (RULES.md §5.3) — see {@link AwsSecretsManagerConfig}.
 */
@ConfigurationProperties(prefix = "adren.aws")
record AwsProperties(String endpointOverride, String region) {

    boolean hasEndpointOverride() {
        return endpointOverride != null && !endpointOverride.isBlank();
    }
}
