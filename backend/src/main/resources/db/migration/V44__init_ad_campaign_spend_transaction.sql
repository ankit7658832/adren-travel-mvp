-- ADS-11: PRD §14.3's billing-transparency requirement — a per-transaction
-- breakdown of ad spend, not just the single running spend_to_date_amount
-- ad_campaign already carries. Extends ADS-10's spend-cap poller to write
-- one row here per poll increment.

CREATE TABLE ad_campaign_spend_transaction (
    transaction_id  UUID PRIMARY KEY,
    campaign_id     UUID NOT NULL REFERENCES ad_campaign (campaign_id),
    amount          NUMERIC(19,4) NOT NULL,
    recorded_at     TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_ad_campaign_spend_transaction_campaign ON ad_campaign_spend_transaction (campaign_id);
