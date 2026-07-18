package com.adren.travel.ai.internal;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeoutException;

/**
 * AI-01 — the single Groq client every AI capability calls through, per PRD
 * §11.1. Groq's API is OpenAI-compatible (chat completions), reached over a
 * REAL {@link WebClient} call (not a hardcoded stub the way {@code
 * HotelbedsClient} etc. are for suppliers pending sandbox credentials) —
 * this module's whole purpose is validating a genuine external LLM
 * integration, so the client must actually reach Groq's API, authenticate,
 * and handle its real response/error shapes, even when {@code
 * adren.ai.groq.api-key} is a placeholder value pending a real key (see
 * this story's own commit message for what was verified against the real
 * API with a deliberately-invalid key).
 * <p>
 * Distinct exception types per failure mode (backend-best-practices §2,
 * mirroring {@code HotelbedsClient.HotelbedsRateExpiredException}) rather
 * than one generic "GroqException with a string reason" — callers (AI-02's
 * grounded-generation flow) need to tell "the key is wrong" apart from
 * "Groq is rate-limiting us" apart from "the call took too long", since
 * each maps to different caller behavior (AI-05's explicit-failure-state
 * requirement, AI-13's retry/timeout audit logging).
 */
@Component
class GroqClient {

    private final WebClient webClient;
    private final GroqProperties properties;

    GroqClient(WebClient.Builder webClientBuilder, GroqProperties properties) {
        this.webClient = webClientBuilder.baseUrl(properties.baseUrl()).build();
        this.properties = properties;
    }

    /**
     * Runs one chat-completion call and returns the assistant's raw content
     * string (expected to be a JSON object when {@code jsonMode} is true —
     * AI-02 parses it further). Bounded by {@code
     * adren.ai.groq.timeout-seconds} (AI-13) — a Groq call that doesn't
     * respond in time throws {@link GroqTimeoutException} rather than
     * hanging the caller past the 10-minute itinerary-build target.
     */
    String chatCompletion(String systemPrompt, String userPrompt, boolean jsonMode) {
        GroqChatCompletionRequest request = new GroqChatCompletionRequest(
            properties.model(),
            List.of(
                new GroqMessage("system", systemPrompt),
                new GroqMessage("user", userPrompt)),
            jsonMode ? new GroqResponseFormat("json_object") : null);

        try {
            GroqChatCompletionResponse response = webClient.post()
                .uri("/chat/completions")
                .header("Authorization", "Bearer " + properties.apiKey())
                .bodyValue(request)
                .retrieve()
                .bodyToMono(GroqChatCompletionResponse.class)
                .timeout(Duration.ofSeconds(properties.timeoutSeconds()))
                .block();

            if (response == null || response.choices() == null || response.choices().isEmpty()) {
                throw new GroqApiException("Groq returned no completion choices");
            }
            return response.choices().get(0).message().content();
        } catch (WebClientResponseException e) {
            throw mapHttpError(e);
        } catch (RuntimeException e) {
            if (isTimeout(e)) {
                throw new GroqTimeoutException(properties.timeoutSeconds(), e);
            }
            throw e;
        }
    }

    private static boolean isTimeout(Throwable e) {
        for (Throwable cause = e; cause != null; cause = cause.getCause()) {
            if (cause instanceof TimeoutException) {
                return true;
            }
        }
        return false;
    }

    private GroqClientException mapHttpError(WebClientResponseException e) {
        int status = e.getStatusCode().value();
        if (status == 401 || status == 403) {
            return new GroqAuthenticationException(status, e);
        }
        if (status == 429) {
            return new GroqRateLimitException(e);
        }
        return new GroqApiException("Groq API call failed with status " + status, e);
    }

    /** Base type for every distinct Groq failure mode — see class Javadoc for why these stay distinct rather than one generic exception. */
    abstract static class GroqClientException extends RuntimeException {
        GroqClientException(String message) {
            super(message);
        }

        GroqClientException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /** The configured API key was rejected — real, observed Groq behavior: {@code 401 {"error":{"code":"invalid_api_key"}}}. */
    static class GroqAuthenticationException extends GroqClientException {
        GroqAuthenticationException(int status, Throwable cause) {
            super("Groq rejected the configured API key (HTTP " + status + ") — GROQ_API_KEY is missing or invalid", cause);
        }
    }

    /** Groq's rate limit was hit — distinct from a hard auth failure so callers can decide whether to retry. */
    static class GroqRateLimitException extends GroqClientException {
        GroqRateLimitException(Throwable cause) {
            super("Groq rate limit exceeded", cause);
        }
    }

    /** {@code adren.ai.groq.timeout-seconds} elapsed before Groq responded (AI-13). */
    static class GroqTimeoutException extends GroqClientException {
        GroqTimeoutException(int timeoutSeconds, Throwable cause) {
            super("Groq call did not complete within " + timeoutSeconds + "s", cause);
        }
    }

    /** Any other Groq failure (5xx, malformed response, empty choices) — never silently swallowed, never fabricated into a suggestion. */
    static class GroqApiException extends GroqClientException {
        GroqApiException(String message) {
            super(message);
        }

        GroqApiException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
