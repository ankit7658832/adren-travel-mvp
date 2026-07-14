package com.adren.travel.security.internal;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Map;

/**
 * Turns an authenticated-but-not-authorized call (wrong role, or a
 * tenant-isolation rejection per RULES.md §5.2) into a plain 403. See
 * {@link RestAuthenticationEntryPoint} for the 401 counterpart and the same
 * "precursor to FND-22" note.
 */
class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    RestAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException)
        throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), Map.of(
            "status", 403,
            "title", "Forbidden",
            "detail", "The authenticated principal is not authorized to perform this action.",
            "instance", request.getRequestURI()
        ));
    }
}
