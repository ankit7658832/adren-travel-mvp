package com.adren.travel.security.internal;

import com.adren.travel.security.CapabilityGrantService.Capability;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface CapabilityGrantRepository extends JpaRepository<CapabilityGrant, UUID> {

    Optional<CapabilityGrant> findByUserIdAndCapability(UUID userId, Capability capability);
}
