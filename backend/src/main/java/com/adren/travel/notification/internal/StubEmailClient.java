package com.adren.travel.notification.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Mock-phase-only {@link EmailClient} — logs rather than calling a real provider, matching FIN-11's {@code StripeClient} stubbing precedent. */
@Component
class StubEmailClient implements EmailClient {

    private static final Logger log = LoggerFactory.getLogger(StubEmailClient.class);

    @Override
    public void send(UUID consultantId, String subject, String body) {
        log.info("Email stub dispatch to consultant={}, subject={}, body={}", consultantId, subject, body);
    }
}
