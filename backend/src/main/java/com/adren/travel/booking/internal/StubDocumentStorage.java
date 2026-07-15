package com.adren.travel.booking.internal;

import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Mock-phase-only {@link DocumentStorage} — generates a fake storage
 * reference rather than uploading to S3/LocalStack, matching FIN-11's
 * {@code StubStripeClient} precedent for external-integration seams in
 * this vertical slice.
 */
@Component
class StubDocumentStorage implements DocumentStorage {

    @Override
    public String store(String keyPrefix, byte[] content) {
        return keyPrefix + "/" + UUID.randomUUID() + ".pdf";
    }
}
