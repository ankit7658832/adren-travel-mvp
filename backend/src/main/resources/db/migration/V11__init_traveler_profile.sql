-- BOK-14 — Traveler Profile (PRD Section 20.10). consultant_id is a
-- foreign key VALUE only, not a constraint across module-owned schemas
-- (RULES.md Section 4.2) — booking references whitelabel's consultant_id
-- the same way supplier's byos_credential table already does.

CREATE TABLE traveler_profile (
    traveler_id       UUID PRIMARY KEY,
    consultant_id     UUID NOT NULL,
    name              VARCHAR(255) NOT NULL,
    date_of_birth     DATE NOT NULL,
    passport_number   VARCHAR(50),
    passport_expiry   DATE,
    nationality       VARCHAR(100),
    created_at        TIMESTAMPTZ NOT NULL,
    updated_at        TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_traveler_profile_consultant_id ON traveler_profile (consultant_id);

-- document_vault[] — encrypted storage references (opaque strings; the
-- actual file encryption is a separate concern, e.g. S3 SSE-KMS, not
-- modeled here), own table since the count varies per traveler.
CREATE TABLE traveler_profile_document (
    traveler_id       UUID NOT NULL REFERENCES traveler_profile(traveler_id),
    document_reference VARCHAR(2048) NOT NULL
);

-- preferences — flexible key/value (meal, seating, accessibility, etc.),
-- own table per RULES.md Section 24.7's data-driven-field convention
-- (mirrors consultant_kyc_field).
CREATE TABLE traveler_profile_preference (
    traveler_id       UUID NOT NULL REFERENCES traveler_profile(traveler_id),
    preference_key    VARCHAR(100) NOT NULL,
    preference_value  VARCHAR(500),
    PRIMARY KEY (traveler_id, preference_key)
);
