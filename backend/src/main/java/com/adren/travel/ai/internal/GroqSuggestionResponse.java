package com.adren.travel.ai.internal;

import java.util.List;

/**
 * The structured JSON contract the system prompt requires Groq to respond
 * with (see {@code AiServiceImpl.SYSTEM_PROMPT}). Deliberately minimal:
 * the model returns ONLY which {@code supplierRateId}s it's selecting from
 * the candidate list it was given, plus a viability verdict — never a
 * property name, price, or any other descriptive field, which the model
 * could otherwise alter/invent even while nominally "grounded." Every
 * descriptive field the caller sees comes from the real {@code
 * SupplierSearchResult} matching the returned id, not from this response —
 * see {@code AiServiceImpl#validateAndGround} for where that's enforced.
 */
record GroqSuggestionResponse(List<String> selectedSupplierRateIds, boolean viable, String reason) {
}
