package com.adren.travel.ai.internal;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds {@code adren.ai.groq.*} from application.yml (PRD §11.1, AI-01).
 * The API key itself is deliberately NOT a field here — {@link
 * GroqApiKeyResolver} (OPS-07) resolves it from Secrets Manager instead,
 * per RULES.md §5.3's "never a plaintext config value" rule.
 * {@code timeoutSeconds}/{@code maxRetries} (AI-13, PRD §24.3/§9.6) bound
 * how long any single Groq call is allowed to run so a slow/hung call
 * can't blow the 10-minute itinerary-build target.
 */
@ConfigurationProperties(prefix = "adren.ai.groq")
record GroqProperties(String baseUrl, String model, int timeoutSeconds, int maxRetries) {
}
