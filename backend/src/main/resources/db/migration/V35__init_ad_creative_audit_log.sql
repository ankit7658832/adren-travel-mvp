-- AI-12, PRD S14.4 / S24.3 / RULES.md S6.3: a dedicated, insert-only audit
-- trail for every ad-creative generation attempt (produced or not) --
-- package-scoped rather than itinerary-scoped, so it's a separate table
-- from ai_suggestion_audit_log (V32) rather than a nullable itinerary_id
-- bolted onto that one.

CREATE TABLE ad_creative_audit_log (
    audit_log_id                UUID PRIMARY KEY,
    consultant_id                 UUID NOT NULL,
    package_id                     UUID NOT NULL,
    request_input_json              TEXT NOT NULL,
    source_data_snapshot_json        TEXT NOT NULL,
    ai_output_json                    TEXT,
    disposition                        VARCHAR(30) NOT NULL,
    created_at                          TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_ad_creative_audit_log_package_id ON ad_creative_audit_log (package_id);
CREATE INDEX idx_ad_creative_audit_log_consultant_id ON ad_creative_audit_log (consultant_id);
