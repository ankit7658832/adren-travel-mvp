package com.adren.travel.security.internal;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Map;

/**
 * Turns "no authenticated principal reached a non-public endpoint" into a
 * plain 401, per FND-01's acceptance criterion. The body shape here is a
 * minimal precursor to FND-22's full RFC 7807 {@code ProblemDetailFactory} —
 * intentionally not duplicating that story's scope, just not leaving this
 * response bodiless in the meantime.
 */
class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    RestAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
        throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), Map.of(
            "status", 401,
            "title", "Unauthorized",
            "detail", "A valid bearer token is required to access this resource.",
            "instance", request.getRequestURI()
        ));
    }
}
