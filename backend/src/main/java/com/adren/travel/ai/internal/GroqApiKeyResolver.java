package com.adren.travel.ai.internal;

/**
 * OPS-07 — resolves the Groq API key {@link GroqClient} authenticates with.
 * A plain interface (not folded into {@link GroqProperties}) so
 * {@code GroqClientTest} can supply a trivial fake instead of standing up a
 * real {@code SecretsManagerClient}.
 */
interface GroqApiKeyResolver {

    String resolveApiKey();
}
