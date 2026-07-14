package com.adren.travel.shared;

/**
 * The MDC key every module logs its correlation ID under (RULES.md §6.1) —
 * a plain constant, not a service, so any module can log/read it without
 * importing another module's {@code .internal} package.
 */
public final class TraceIds {

    public static final String MDC_KEY = "traceId";
    public static final String HEADER = "X-Adren-Trace-Id";

    private TraceIds() {
    }
}
