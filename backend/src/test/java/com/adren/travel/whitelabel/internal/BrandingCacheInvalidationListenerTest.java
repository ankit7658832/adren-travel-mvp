package com.adren.travel.whitelabel.internal;

import com.adren.travel.whitelabel.event.BrandingUpdatedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.verify;

/** FND-07/FND-08 — the cache-invalidation reaction to a committed branding save. */
@ExtendWith(MockitoExtension.class)
class BrandingCacheInvalidationListenerTest {

    @Mock
    BrandingCache brandingCache;

    @Mock
    RegisteredDomainsCache registeredDomainsCache;

    @Test
    void evictsTheSavedConsultantsCacheEntryAndInvalidatesTheDomainRegistry() {
        BrandingCacheInvalidationListener listener =
            new BrandingCacheInvalidationListener(brandingCache, registeredDomainsCache);
        UUID consultantId = UUID.randomUUID();

        listener.on(new BrandingUpdatedEvent(consultantId, "consultant.example.com"));

        verify(brandingCache).evict(consultantId);
        verify(registeredDomainsCache).invalidate();
    }
}
