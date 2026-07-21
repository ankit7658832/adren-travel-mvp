package com.adren.travel.ai.internal;

import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;

/**
 * OPS-07 — the Groq API key, same RULES.md §5.3 pattern as
 * {@code AwsSupplierSecretsService} (FND-11): resolved from Secrets Manager
 * by name, never bound as a plaintext config value. {@link
 * com.adren.travel.AwsSecretsManagerConfig}'s {@code SecretsManagerClient}
 * bean already points at LocalStack when {@code adren.aws.endpoint-override}
 * is set (local/test) and real Secrets Manager otherwise — this resolver
 * doesn't branch on environment/profile itself, the same way
 * {@code AwsSupplierSecretsService} doesn't. {@code docker-compose.yml}
 * seeds {@link #SECRET_NAME} locally (from {@code GROQ_API_KEY} if set, a
 * placeholder otherwise) so local dev keeps working without a plaintext
 * value ever reaching this module's own config binding.
 * <p>
 * Resolved once at construction (not per-call, unlike {@code
 * SupplierCredentialResolver}'s per-search resolution) — {@link GroqClient}
 * calls Groq far more often than a supplier search happens, and AI-13
 * already bounds total Groq latency; an extra unbounded Secrets Manager
 * round trip on every single chat-completion call isn't a cost this story
 * should introduce.
 */
@Component
class SecretsManagerGroqApiKeyResolver implements GroqApiKeyResolver {

    private static final String SECRET_NAME = "adren/ai/groq-api-key";

    private final SecretsManagerClient secretsManagerClient;

    SecretsManagerGroqApiKeyResolver(SecretsManagerClient secretsManagerClient) {
        this.secretsManagerClient = secretsManagerClient;
    }

    @Override
    public String resolveApiKey() {
        return secretsManagerClient.getSecretValue(GetSecretValueRequest.builder()
            .secretId(SECRET_NAME)
            .build()).secretString();
    }
}
