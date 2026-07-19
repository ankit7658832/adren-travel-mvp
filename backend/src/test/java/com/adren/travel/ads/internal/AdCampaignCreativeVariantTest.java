package com.adren.travel.ads.internal;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** ADS-05 — the per-variant approval flag. */
class AdCampaignCreativeVariantTest {

    @Test
    void startsUnapprovedAndApproveFlipsItADS05() {
        AdCampaignCreativeVariant variant = new AdCampaignCreativeVariant(
            UUID.randomUUID(), UUID.randomUUID(), "Escape to Goa", "Book now", null);

        assertThat(variant.isApproved()).isFalse();

        variant.approve();

        assertThat(variant.isApproved()).isTrue();
    }
}
