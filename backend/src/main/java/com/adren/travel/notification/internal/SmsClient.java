package com.adren.travel.notification.internal;

import java.util.UUID;

/** Seam over an SMS provider (PRD §15/§22.7 T11, HRD-01) — see {@link EmailClient}'s Javadoc for the same {@code consultantId}-not-address rationale. */
interface SmsClient {

    void send(UUID consultantId, String message);
}
