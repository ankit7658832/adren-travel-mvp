-- ADS-01: one Adren-managed Meta ad account/Business Manager per
-- Consultant (PRD §14.1, §6 "No (executes)" row — never Consultant-owned).

CREATE TABLE ad_account (
    ad_account_id          UUID PRIMARY KEY,
    consultant_id          UUID NOT NULL,
    meta_business_manager_id VARCHAR(100) NOT NULL,
    provisioned_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_ad_account_consultant UNIQUE (consultant_id)
);
