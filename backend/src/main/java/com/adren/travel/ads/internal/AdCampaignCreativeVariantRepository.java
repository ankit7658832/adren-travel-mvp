package com.adren.travel.ads.internal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface AdCampaignCreativeVariantRepository extends JpaRepository<AdCampaignCreativeVariant, UUID> {

    List<AdCampaignCreativeVariant> findByCampaignId(UUID campaignId);
}
