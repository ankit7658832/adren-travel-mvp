package com.adren.travel.shared;

/**
 * The MDC keys every module logs its request/event context under (RULES.md
 * §6.2) — plain constants, not a service, so any module can populate/read
 * them without importing another module's {@code .internal} package.
 * {@link TraceIds#MDC_KEY} is the correlation ID counterpart of this set.
 */
public final class LogFields {

    /** The tenant a log line concerns; absent only for a SUPER_ADMIN-scoped request/event. */
    public static final String CONSULTANT_ID = "consultantId";

    /** Mandatory alongside any monetary amount in the same structured field set (RULES.md §6.2/§4.4). */
    public static final String CURRENCY = "currency";

    /** Mandatory on any compliance-relevant log line (GST/TCS, ATOL, KYC) — per-market rules diverge (PRD §17, §24.7). */
    public static final String MARKET = "market";

    private LogFields() {
    }
}
