package com.adren.travel.ai.internal;

/** What became of one AI suggestion attempt (PRD §11.2 principle 5 — the audit trail records disposition, not just output). */
enum AiSuggestionDisposition {
    /** A grounded suggestion was produced and returned to the caller. */
    SUGGESTED,
    /** The model (or this module's own grounding validation) found no candidate satisfying the request/budget — PRD §11.2 principle 4. */
    NO_VIABLE_SUGGESTION,
    /** The Groq call itself failed (auth/rate-limit/timeout/other) — logged distinctly per AI-13, never silently dropped. */
    GROQ_ERROR
}
