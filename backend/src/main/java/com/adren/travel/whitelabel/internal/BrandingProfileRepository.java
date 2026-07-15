package com.adren.travel.whitelabel.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

interface BrandingProfileRepository extends JpaRepository<BrandingProfile, UUID> {

    /**
     * FND-08's dynamic CORS allow-list source — every currently-mapped
     * CNAME domain, never the full entity (RegisteredDomainsCache holds
     * only this set, not branding content, so it doesn't duplicate
     * {@link BrandingCache}'s per-consultant cache).
     */
    @Query("select b.domain from BrandingProfile b where b.domain is not null")
    List<String> findAllDomains();
}
