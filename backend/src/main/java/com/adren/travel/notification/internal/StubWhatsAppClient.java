package com.adren.travel.notification.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Mock-phase-only {@link WhatsAppClient} — logs rather than calling a real provider, matching FIN-11's {@code StripeClient} stubbing precedent. */
@Component
class StubWhatsAppClient implements WhatsAppClient {

    private static final Logger log = LoggerFactory.getLogger(StubWhatsAppClient.class);

    @Override
    public void send(UUID consultantId, String message) {
        log.info("WhatsApp stub dispatch to consultant={}, message={}", consultantId, message);
    }
}
