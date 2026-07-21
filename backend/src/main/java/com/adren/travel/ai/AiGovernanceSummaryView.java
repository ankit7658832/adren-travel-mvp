package com.adren.travel.ai;

/** HRD-11, PRD §9.5/§21.6 — the Super Admin Dashboard's AI governance summary, platform scope. */
public record AiGovernanceSummaryView(
    long totalSuggestions,
    long suggestedCount,
    long noViableSuggestionCount,
    long groqErrorCount
) {
}
