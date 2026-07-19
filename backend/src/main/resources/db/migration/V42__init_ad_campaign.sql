-- ADS-02: PRD §20.13's Ad Campaign data dictionary + status state machine
-- (PendingApproval -> PendingPolicyReview -> Live -> Paused/Rejected/
-- SpendCapReached). version is optimistic locking, matching BOK-16's
-- precedent for a finite/contended resource — ADS-10's near-real-time
-- spend-cap enforcement writes spend_to_date_amount under concurrent
-- campaign activity.

CREATE TABLE ad_campaign (
    campaign_id             UUID PRIMARY KEY,
    package_id              UUID NOT NULL,
    consultant_id           UUID NOT NULL,
    status                  VARCHAR(30) NOT NULL,
    audience_description    TEXT,
    budget_cap_amount       NUMERIC(19,4),
    budget_cap_currency     VARCHAR(10) NOT NULL,
    duration_days           INTEGER,
    meta_campaign_ref       VARCHAR(100),
    spend_to_date_amount    NUMERIC(19,4) NOT NULL DEFAULT 0,
    rejection_reason        TEXT,
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at              TIMESTAMP WITH TIME ZONE NOT NULL,
    version                 BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_ad_campaign_consultant ON ad_campaign (consultant_id);

-- creative_variants[] (PRD §20.13) — one row per AI-generated variant,
-- each individually Consultant-approved (ADS-05).
CREATE TABLE ad_campaign_creative_variant (
    variant_id    UUID PRIMARY KEY,
    campaign_id   UUID NOT NULL REFERENCES ad_campaign (campaign_id),
    headline      VARCHAR(200) NOT NULL,
    body_text     TEXT NOT NULL,
    image_ref     VARCHAR(200),
    approved      BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_ad_campaign_creative_variant_campaign ON ad_campaign_creative_variant (campaign_id);
