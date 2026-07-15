package com.adren.travel.whitelabel.internal;

import com.adren.travel.whitelabel.event.BrandingUpdatedEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * FND-07/FND-08 — evicts a Consultant's cached branding and invalidates
 * the CORS domain-registry cache the moment their save actually commits
 * (not before — evicting pre-commit could let a reader repopulate a cache
 * with the about-to-be-superseded row if the transaction then rolled
 * back). This is an in-module reaction to the module's own event, so a
 * plain {@code @TransactionalEventListener} (synchronous, same JVM) is
 * enough — unlike a cross-module listener (e.g. {@code notification}'s),
 * this never needs {@code @Async} or the event-publication registry's
 * cross-module delivery guarantee.
 */
@Component
class BrandingCacheInvalidationListener {

    private final BrandingCache brandingCache;
    private final RegisteredDomainsCache registeredDomainsCache;

    BrandingCacheInvalidationListener(BrandingCache brandingCache, RegisteredDomainsCache registeredDomainsCache) {
        this.brandingCache = brandingCache;
        this.registeredDomainsCache = registeredDomainsCache;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void on(BrandingUpdatedEvent event) {
        brandingCache.evict(event.consultantId());
        registeredDomainsCache.invalidate();
    }
}
