package com.adren.travel.ads.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * One AI-generated creative variant persisted against a campaign (PRD
 * §20.13's {@code creative_variants[]}, ADS-04) — package-private, own
 * table (not an embedded collection on {@link AdCampaign}, since each
 * variant is individually Consultant-approved, ADS-05).
 */
@Entity
@Table(name = "ad_campaign_creative_variant")
class AdCampaignCreativeVariant {

    @Id
    private UUID variantId;

    private UUID campaignId;
    private String headline;

    @Column(name = "body_text")
    private String bodyText;

    @Column(name = "image_ref")
    private String imageRef;

    private boolean approved;

    protected AdCampaignCreativeVariant() {
        // JPA
    }

    AdCampaignCreativeVariant(UUID variantId, UUID campaignId, String headline, String bodyText, String imageRef) {
        this.variantId = variantId;
        this.campaignId = campaignId;
        this.headline = headline;
        this.bodyText = bodyText;
        this.imageRef = imageRef;
        this.approved = false;
    }

    /** ADS-05 — a Consultant approves this one variant. */
    void approve() {
        this.approved = true;
    }

    UUID getVariantId() {
        return variantId;
    }

    UUID getCampaignId() {
        return campaignId;
    }

    String getHeadline() {
        return headline;
    }

    String getBodyText() {
        return bodyText;
    }

    String getImageRef() {
        return imageRef;
    }

    boolean isApproved() {
        return approved;
    }
}
