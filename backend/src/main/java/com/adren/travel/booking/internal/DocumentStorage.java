package com.adren.travel.booking.internal;

/**
 * Seam over document storage (PRD §20.11, BOK-15 — "stored to LocalStack
 * S3 in MVP") — this mock phase ships only {@link StubDocumentStorage};
 * a real implementation uploading to S3/LocalStack (OPS-01/OPS-03) is
 * production-tier work, swapped in behind this same interface without
 * touching {@code VoucherService}.
 */
interface DocumentStorage {

    /** Stores {@code content} and returns its storage reference (an S3 key in a real implementation). */
    String store(String keyPrefix, byte[] content);

    /** SCR-17 — retrieves previously-stored content by the reference {@link #store} returned. */
    byte[] retrieve(String reference);
}
