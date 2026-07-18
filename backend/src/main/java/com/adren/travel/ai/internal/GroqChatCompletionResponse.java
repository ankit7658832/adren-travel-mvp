package com.adren.travel.ai.internal;

import java.util.List;

/** Wire shape of a Groq (OpenAI-compatible) chat-completions response — see {@link GroqClient}. Only the fields this client actually reads are modeled. */
record GroqChatCompletionResponse(List<GroqChoice> choices) {

    record GroqChoice(GroqMessage message) {
    }
}
