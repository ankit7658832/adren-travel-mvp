-- AI-08, PRD S23.3 Edge Case #8 / S25 T14: the original AI output
-- (ai_suggestion_audit_log, V32, never modified) must never be overwritten
-- by an edit -- the edited final version is a SEPARATE linked, insert-only
-- row, not an UPDATE to the original.
--
-- suggested_line_items_json (added to the original table) is the exact
-- grounded AiSuggestedLineItem list returned to the caller at generation
-- time, distinct from ai_output_json (the model's raw response) -- AI-08
-- compares THIS against the edited final version, an apples-to-apples
-- structural comparison neither the raw model text nor the full candidate
-- snapshot would give cleanly.
ALTER TABLE ai_suggestion_audit_log ADD COLUMN suggested_line_items_json TEXT;

CREATE TABLE ai_suggestion_approval (
    approval_id                UUID PRIMARY KEY,
    audit_log_id                 UUID NOT NULL,
    approved_by_user_id            UUID NOT NULL,
    edited_final_version_json        TEXT NOT NULL,
    was_edited                         BOOLEAN NOT NULL,
    approved_at                          TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_ai_suggestion_approval_audit_log_id ON ai_suggestion_approval (audit_log_id);
