package com.adren.travel.ads.internal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface AdCampaignRepository extends JpaRepository<AdCampaign, UUID> {

    /** ADS-06 — the Super Admin's brand-safety/policy review queue. */
    Page<AdCampaign> findByStatus(AdCampaignStatus status, Pageable pageable);

    /** ADS-09 — the Consultant Dashboard's Active Campaigns tab. */
    Page<AdCampaign> findByConsultantId(UUID consultantId, Pageable pageable);

    /** ADS-10's spend-cap poller / ADS-09's performance-feed poller — every campaign the mock feeds must visit this run. */
    List<AdCampaign> findByStatus(AdCampaignStatus status);

    /**
     * ADS-12 — the Live campaign(s) promoting a given Package, the moment
     * its price changes. {@code List} rather than a single optional
     * result: PRD §20.13 doesn't forbid more than one campaign ever
     * referencing the same {@code package_id} over a Package's lifetime
     * (a rejected/paused campaign's replacement), so this must not assume
     * uniqueness.
     */
    List<AdCampaign> findByPackageIdAndStatus(UUID packageId, AdCampaignStatus status);

    /**
     * ADS-13 — every campaign the mocked Meta suspension signal must
     * flag: everything under the Consultant except REJECTED (a rejected
     * campaign never spends, so "action required" would be meaningless
     * for it).
     */
    List<AdCampaign> findByConsultantIdAndStatusNot(UUID consultantId, AdCampaignStatus excludedStatus);
}
