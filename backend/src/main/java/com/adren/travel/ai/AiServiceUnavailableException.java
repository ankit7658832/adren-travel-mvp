package com.adren.travel.ai;

/**
 * Thrown when a Groq call fails after exhausting AI-13's bounded retries
 * (timeout, rate-limit) or hits a non-retryable failure (auth/config) —
 * the PUBLIC translation of {@code ai.internal.GroqClient}'s exception
 * family, which must never cross this module's boundary directly
 * (RULES.md §4.1: {@code internal} types are not part of the public
 * contract). Stage 4 Step C adversarial finding: before this type existed,
 * the raw internal exception propagated unmapped past every {@code
 * @ControllerAdvice} in the app (none of which could reference an {@code
 * internal} type to catch it), producing whatever Spring's default error
 * handling did with an unmapped {@code RuntimeException} — the message
 * here is deliberately generic (no raw Groq error text), since the full
 * detail is always in the audit trail (AI-07, 100% logged) for actual
 * diagnosis, not the client response (RULES.md §6.2).
 */
public class AiServiceUnavailableException extends RuntimeException {

    public AiServiceUnavailableException() {
        super("AI suggestions are temporarily unavailable — please try again shortly.");
    }
}
