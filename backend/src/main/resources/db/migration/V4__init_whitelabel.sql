-- White-Label & Admin Console module (PRD Section 13.1 — Consultant
-- onboarding / per-market KYC). One file per module's first entity, per
-- RULES.md Section 4.2 — module-owned tables only.

CREATE TABLE consultant (
    consultant_id UUID PRIMARY KEY,
    business_name VARCHAR(255) NOT NULL,
    home_market   VARCHAR(30) NOT NULL,
    status        VARCHAR(20) NOT NULL,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL
);

-- Flexible per-market KYC field storage (RULES.md Section 24.7 — the
-- required-field set is data-driven per market, not a fixed column set).
CREATE TABLE consultant_kyc_field (
    consultant_id UUID NOT NULL REFERENCES consultant(consultant_id),
    field_key     VARCHAR(100) NOT NULL,
    field_value   VARCHAR(500),
    PRIMARY KEY (consultant_id, field_key)
);
