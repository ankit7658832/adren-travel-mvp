package com.adren.travel.security.internal;

import com.adren.travel.security.CapabilityGrantService;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Implementation of {@link CapabilityGrantService}, registered under the
 * bean name {@code capabilityGrantService} so {@code @PreAuthorize}
 * expressions elsewhere can call {@code @capabilityGrantService.isGranted(...)}.
 */
@Service("capabilityGrantService")
class CapabilityGrantServiceImpl implements CapabilityGrantService {

    private final CapabilityGrantRepository repository;

    CapabilityGrantServiceImpl(CapabilityGrantRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean isGranted(UUID userId, Capability capability) {
        return repository.findByUserIdAndCapability(userId, capability)
            .map(CapabilityGrant::isGranted)
            .orElse(false);
    }
}
