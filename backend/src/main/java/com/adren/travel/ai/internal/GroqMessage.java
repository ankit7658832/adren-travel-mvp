package com.adren.travel.ai.internal;

/** One chat message — {@code role} is {@code "system"}, {@code "user"}, or {@code "assistant"}. */
record GroqMessage(String role, String content) {
}
