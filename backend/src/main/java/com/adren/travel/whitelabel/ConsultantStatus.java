package com.adren.travel.whitelabel;

/**
 * A Consultant's lifecycle status (PRD §3.1/§21.6, FND-05). Public — the
 * Super Admin Console reads/sets this via {@link WhitelabelApi}, and
 * {@code booking}'s search/booking entry points check it via
 * {@link WhitelabelApi#requireConsultantActive(java.util.UUID)} before
 * letting a Consultant's Users proceed.
 */
public enum ConsultantStatus {
    ACTIVE, SUSPENDED
}
