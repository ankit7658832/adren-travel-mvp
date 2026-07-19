package com.adren.travel.ads.internal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface AdCampaignRepository extends JpaRepository<AdCampaign, UUID> {
}
