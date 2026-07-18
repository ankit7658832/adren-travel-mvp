-- Supplier module, DMC-03/DMC-10/DMC-11, PRD S10.2.8: a Local DMC's
-- manually-catalogued products (tours, transfers, activities) -- bulk
-- inserted via a validated CSV upload (DMC-03), individually editable
-- afterward (DMC-10), and checked for staleness on updated_at (DMC-11).

CREATE TABLE local_dmc_inventory_item (
    item_id                     UUID PRIMARY KEY,
    local_dmc_id                  UUID NOT NULL REFERENCES local_dmc_record(local_dmc_id),
    product_name                    VARCHAR(255) NOT NULL,
    category                          VARCHAR(20) NOT NULL,
    net_rate                            NUMERIC(19,4) NOT NULL,
    net_rate_currency                     VARCHAR(10) NOT NULL,
    cancellation_policy_text                TEXT NOT NULL,
    available_from                            DATE NOT NULL,
    available_to                                DATE NOT NULL,
    created_at                                    TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at                                      TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_local_dmc_inventory_item_local_dmc_id ON local_dmc_inventory_item (local_dmc_id);
