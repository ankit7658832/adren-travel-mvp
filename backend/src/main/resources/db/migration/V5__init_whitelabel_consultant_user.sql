-- FND-09: Consultant Users (staff/sub-agents) under their own account
-- (PRD Section 3.3). Capability grants live in security's own
-- capability_grant table (V2), keyed by this table's user_id.

CREATE TABLE consultant_user (
    user_id       UUID PRIMARY KEY,
    consultant_id UUID NOT NULL REFERENCES consultant(consultant_id),
    email         VARCHAR(255) NOT NULL,
    display_name  VARCHAR(255) NOT NULL,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_consultant_user_consultant_id ON consultant_user (consultant_id);
