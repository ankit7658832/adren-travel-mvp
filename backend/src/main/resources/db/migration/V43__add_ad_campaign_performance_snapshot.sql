-- ADS-09: PRD §20.13's performance_snapshot {impressions, clicks,
-- bookings_attributed} flowed back to the Consultant Dashboard (§14.2
-- step 7). Columns on ad_campaign itself rather than a child table since
-- this is a single running snapshot per campaign, not a history of
-- discrete events.

ALTER TABLE ad_campaign
    ADD COLUMN impressions          INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN clicks                INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN bookings_attributed    INTEGER NOT NULL DEFAULT 0;
