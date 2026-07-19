package com.adren.travel.ads.internal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface AdCampaignRepository extends JpaRepository<AdCampaign, UUID> {

    /** ADS-06 — the Super Admin's brand-safety/policy review queue. */
    Page<AdCampaign> findByStatus(AdCampaignStatus status, Pageable pageable);
}
