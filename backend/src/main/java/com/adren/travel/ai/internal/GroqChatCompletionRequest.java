package com.adren.travel.ai.internal;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** Wire shape of a Groq (OpenAI-compatible) chat-completions request — see {@link GroqClient}. */
record GroqChatCompletionRequest(
    String model,
    List<GroqMessage> messages,
    @JsonProperty("response_format") GroqResponseFormat responseFormat
) {
}
