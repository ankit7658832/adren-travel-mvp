package com.adren.travel.security.internal;

import com.adren.travel.shared.TraceIds;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Generates a correlation ID at the edge (RULES.md §6.1) — before Spring
 * Security, so it's present even on a 401/403 response — and puts it in
 * MDC for the duration of the request. Echoed back as a response header so
 * a Consultant-reported error's traceId is visible client-side too, and it
 * must match the {@code traceId} field in any RFC 7807 error body (FND-22).
 */
class TraceIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        String incoming = request.getHeader(TraceIds.HEADER);
        String traceId = (incoming != null && !incoming.isBlank()) ? incoming : UUID.randomUUID().toString();
        MDC.put(TraceIds.MDC_KEY, traceId);
        response.setHeader(TraceIds.HEADER, traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(TraceIds.MDC_KEY);
        }
    }
}
