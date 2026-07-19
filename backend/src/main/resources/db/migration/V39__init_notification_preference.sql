-- HRD-04 — a Consultant's optional override of HRD-01's regional secondary-
-- channel default. Absence of a row (not a nullable column with NULL) means
-- "no override, use the market default" — NotificationPreferenceRepository
-- returns Optional.empty() for a Consultant who has never saved one.
CREATE TABLE notification_preference (
    consultant_id UUID PRIMARY KEY,
    secondary_channel VARCHAR(20) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);
