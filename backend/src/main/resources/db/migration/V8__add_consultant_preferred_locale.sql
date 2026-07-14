-- PRD Section 13.3 (FND-17) — a Consultant's preferred display language,
-- alongside the existing per-market currency/KYC data-driven pattern.
-- English is every market's primary/default language.

ALTER TABLE consultant
    ADD COLUMN preferred_locale VARCHAR(10) NOT NULL DEFAULT 'EN';
