-- ADS-15: PRD §14.3 — a rule-based brand-safety policy template check
-- runs ahead of the Super Admin's manual policy-review queue (ADS-06),
-- flagging obvious violations for the reviewer without auto-rejecting.

ALTER TABLE ad_campaign
    ADD COLUMN policy_template_flagged      BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN policy_template_flag_reason  TEXT;
