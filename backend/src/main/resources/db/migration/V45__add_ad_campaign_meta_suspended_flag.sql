-- ADS-13: PRD §23.5 Edge Case #12 / §25 T17 — a mocked Meta ad-account
-- suspension signal fans out to every non-terminal campaign under the
-- affected Consultant, flagging each one "suspended - action required"
-- rather than letting it silently stop spending with no explanation.
-- AdCampaignStatus (V42) is a closed enum with no SUSPENDED value — this
-- is a flag orthogonal to status, not a new status.

ALTER TABLE ad_campaign
    ADD COLUMN meta_suspended BOOLEAN NOT NULL DEFAULT FALSE;
