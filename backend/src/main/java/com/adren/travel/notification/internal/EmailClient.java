package com.adren.travel.notification.internal;

import java.util.UUID;

/**
 * Seam over an email provider (PRD §15, HRD-01) — this mock phase ships
 * only {@link StubEmailClient}; a real provider integration is
 * production-tier work, swapped in behind this same interface. Takes a
 * {@code consultantId}, not a raw address: no email-address lookup exists
 * yet anywhere in this codebase (Consultant has no email field; only
 * individual {@code ConsultantUser}s do, and resolving "who to notify for
 * a booking" is itself undecided) — a real implementation resolves the
 * actual recipient address at the point it's wired in.
 */
interface EmailClient {

    void send(UUID consultantId, String subject, String body);
}
