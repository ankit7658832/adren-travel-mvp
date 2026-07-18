-- AI-02/AI-07, PRD S11.2 principle 5 / S24.3 / RULES.md S6.3: a dedicated,
-- insert-only audit trail for every AI suggestion (generated or not) --
-- distinct from application logs, which may be sampled/rotated. No
-- UPDATE/DELETE path exists anywhere in the application for this table by
-- design (see AiSuggestionAuditLog.java's Javadoc).
--
-- correlation_id groups every attempt of the SAME logical generation
-- request (AI-13's retry/timeout attempts each get their own row rather
-- than overwriting a "latest attempt" column).

CREATE TABLE ai_suggestion_audit_log (
    audit_log_id               UUID PRIMARY KEY,
    correlation_id              UUID NOT NULL,
    attempt_number               INTEGER NOT NULL,
    consultant_id                 UUID NOT NULL,
    itinerary_id                   UUID NOT NULL,
    request_input_json              TEXT NOT NULL,
    source_data_snapshot_json        TEXT NOT NULL,
    ai_output_json                    TEXT,
    disposition                        VARCHAR(30) NOT NULL,
    created_at                          TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_ai_suggestion_audit_log_itinerary_id ON ai_suggestion_audit_log (itinerary_id);
CREATE INDEX idx_ai_suggestion_audit_log_consultant_id ON ai_suggestion_audit_log (consultant_id);
CREATE INDEX idx_ai_suggestion_audit_log_correlation_id ON ai_suggestion_audit_log (correlation_id);
