package com.adren.travel.booking.internal;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mock-phase-only {@link DocumentStorage} — generates a fake storage
 * reference rather than uploading to S3/LocalStack, matching FIN-11's
 * {@code StubStripeClient} precedent for external-integration seams in
 * this vertical slice. Unlike most stubs in this codebase, this one keeps
 * an in-memory map of what it "stored" — SCR-17's Download Voucher button
 * needs a real (if not durably persisted) round trip, not just a fake
 * reference string nothing can ever read back.
 */
@Component
class StubDocumentStorage implements DocumentStorage {

    private final Map<String, byte[]> storage = new ConcurrentHashMap<>();

    @Override
    public String store(String keyPrefix, byte[] content) {
        String reference = keyPrefix + "/" + UUID.randomUUID() + ".pdf";
        storage.put(reference, content);
        return reference;
    }

    @Override
    public byte[] retrieve(String reference) {
        byte[] content = storage.get(reference);
        if (content == null) {
            throw new IllegalArgumentException("No stored document for reference: " + reference);
        }
        return content;
    }
}
