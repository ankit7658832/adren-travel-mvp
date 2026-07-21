package com.adren.travel.ai.internal;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * AI-01's own sub-task: unit test with mocked HTTP, not a real network call
 * (that's what a real {@code GROQ_API_KEY} run against the live API is
 * for). Mocks at {@link WebClient}'s {@code ExchangeFunction} boundary
 * rather than adding a new mock-server dependency — Spring's own testing
 * surface is enough here.
 */
class GroqClientTest {

    private static final GroqProperties PROPERTIES =
        new GroqProperties("https://api.groq.com/openai/v1", "llama-3.3-70b-versatile", 2, 2);
    private static final GroqApiKeyResolver API_KEY_RESOLVER = () -> "test-key";

    @Test
    void returnsTheAssistantMessageContentFromASuccessfulCompletion() {
        AtomicReference<String> capturedAuthHeader = new AtomicReference<>();
        WebClient.Builder builder = WebClient.builder().exchangeFunction(request -> {
            capturedAuthHeader.set(request.headers().getFirst("Authorization"));
            return Mono.just(ClientResponse.create(HttpStatus.OK)
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .body("""
                    {"choices":[{"message":{"role":"assistant","content":"{\\"lineItems\\":[]}"}}]}
                    """)
                .build());
        });
        GroqClient client = new GroqClient(builder, PROPERTIES, API_KEY_RESOLVER);

        String result = client.chatCompletion("system prompt", "user prompt", true);

        assertThat(result).isEqualTo("{\"lineItems\":[]}");
        assertThat(capturedAuthHeader.get()).isEqualTo("Bearer test-key");
    }

    @Test
    void throwsAGroqAuthenticationExceptionOnA401MatchingTheRealApisObservedShape() {
        WebClient.Builder builder = WebClient.builder().exchangeFunction(request ->
            Mono.just(ClientResponse.create(HttpStatus.UNAUTHORIZED)
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                // Verified against the real Groq API with a deliberately-invalid
                // key (see this story's commit message) — this is the actual
                // observed response shape, not a guess.
                .body("""
                    {"error":{"message":"Invalid API Key","type":"invalid_request_error","code":"invalid_api_key"}}
                    """)
                .build()));
        GroqClient client = new GroqClient(builder, PROPERTIES, API_KEY_RESOLVER);

        assertThatThrownBy(() -> client.chatCompletion("s", "u", true))
            .isInstanceOf(GroqClient.GroqAuthenticationException.class)
            .hasMessageContaining("GROQ_API_KEY");
    }

    @Test
    void throwsAGroqRateLimitExceptionOnA429() {
        WebClient.Builder builder = WebClient.builder().exchangeFunction(request ->
            Mono.just(ClientResponse.create(HttpStatus.TOO_MANY_REQUESTS).build()));
        GroqClient client = new GroqClient(builder, PROPERTIES, API_KEY_RESOLVER);

        assertThatThrownBy(() -> client.chatCompletion("s", "u", true))
            .isInstanceOf(GroqClient.GroqRateLimitException.class);
    }

    @Test
    void throwsAGroqApiExceptionOnA500() {
        WebClient.Builder builder = WebClient.builder().exchangeFunction(request ->
            Mono.just(ClientResponse.create(HttpStatus.INTERNAL_SERVER_ERROR).build()));
        GroqClient client = new GroqClient(builder, PROPERTIES, API_KEY_RESOLVER);

        assertThatThrownBy(() -> client.chatCompletion("s", "u", true))
            .isInstanceOf(GroqClient.GroqApiException.class);
    }

    @Test
    void throwsAGroqTimeoutExceptionWhenTheResponseTakesLongerThanTheConfiguredTimeoutAI13() {
        GroqProperties shortTimeout = new GroqProperties(
            "https://api.groq.com/openai/v1", "llama-3.3-70b-versatile", 1, 2);
        WebClient.Builder builder = WebClient.builder().exchangeFunction(request ->
            Mono.just(ClientResponse.create(HttpStatus.OK)
                    .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .body("""
                        {"choices":[{"message":{"role":"assistant","content":"too slow"}}]}
                        """)
                    .build())
                .delayElement(Duration.ofSeconds(3)));
        GroqClient client = new GroqClient(builder, shortTimeout, API_KEY_RESOLVER);

        assertThatThrownBy(() -> client.chatCompletion("s", "u", true))
            .isInstanceOf(GroqClient.GroqTimeoutException.class)
            .hasMessageContaining("1s");
    }

    @Test
    void throwsAGroqApiExceptionWhenTheResponseHasNoChoices() {
        WebClient.Builder builder = WebClient.builder().exchangeFunction(request ->
            Mono.just(ClientResponse.create(HttpStatus.OK)
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .body("{\"choices\":[]}")
                .build()));
        GroqClient client = new GroqClient(builder, PROPERTIES, API_KEY_RESOLVER);

        assertThatThrownBy(() -> client.chatCompletion("s", "u", true))
            .isInstanceOf(GroqClient.GroqApiException.class)
            .hasMessageContaining("no completion choices");
    }
}
