-- White-Label & Admin Console module (PRD Section 13.2 — Consultant
-- storefront branding). One row per Consultant; consultant_id is both the
-- primary key and the FK (RULES.md Section 4.2 — module-owned table).

CREATE TABLE branding_profile (
    consultant_id        UUID PRIMARY KEY REFERENCES consultant(consultant_id),
    logo_url             VARCHAR(2048),
    background_image_url VARCHAR(2048),
    background_color     VARCHAR(20) NOT NULL,
    text_color_primary   VARCHAR(20) NOT NULL,
    text_color_secondary VARCHAR(20) NOT NULL,
    domain               VARCHAR(255) UNIQUE,
    updated_at           TIMESTAMP WITH TIME ZONE NOT NULL
);
