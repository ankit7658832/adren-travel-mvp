package com.adren.travel.ai.internal;

/** {@code {"type": "json_object"}} — requests a structured JSON response so AI-02 can parse it without free-text extraction. */
record GroqResponseFormat(String type) {
}
