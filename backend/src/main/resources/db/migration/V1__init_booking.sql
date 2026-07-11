-- Initial schema for the Booking module (PRD Section 20.1).
-- Spring Modulith's event publication registry tables are created
-- automatically via spring.modulith.events.jdbc-schema-initialization
-- (see application.yml) and do not need to be defined here.

CREATE TABLE itinerary (
    itinerary_id      UUID PRIMARY KEY,
    consultant_id     UUID NOT NULL,
    created_by_user_id UUID,
    date_range_start  TIMESTAMPTZ,
    date_range_end    TIMESTAMPTZ,
    status            VARCHAR(20) NOT NULL,
    ai_generated      BOOLEAN NOT NULL DEFAULT FALSE,
    ai_audit_log_id   UUID,
    created_at        TIMESTAMPTZ NOT NULL,
    updated_at        TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_itinerary_consultant_id ON itinerary (consultant_id);
