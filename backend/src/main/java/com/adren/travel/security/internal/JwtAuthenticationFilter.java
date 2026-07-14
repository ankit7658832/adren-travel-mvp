package com.adren.travel.security.internal;

import com.adren.travel.security.AdrenPrincipal;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Stateless JWT authentication: reads {@code Authorization: Bearer <token>},
 * and — if present and valid — attaches an {@link AdrenPrincipal} to the
 * {@link org.springframework.security.core.context.SecurityContextHolder} for
 * the duration of this request only (no session, per RULES.md §5.1).
 * <p>
 * A missing or invalid token is deliberately <em>not</em> an error thrown from
 * this filter — it just leaves the security context empty, so
 * {@code authorizeHttpRequests} downstream (see {@link SecurityConfig}) is the
 * single place that turns "no authenticated principal" into a 401 via
 * {@link RestAuthenticationEntryPoint}. This keeps "missing token" and
 * "malformed/expired token" behaving identically from the client's point of
 * view, which is the simpler and more secure default (no information leak
 * about *why* auth failed).
 */
class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenService jwtTokenService;

    JwtAuthenticationFilter(JwtTokenService jwtTokenService) {
        this.jwtTokenService = jwtTokenService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            String token = header.substring(BEARER_PREFIX.length());
            try {
                AdrenPrincipal principal = jwtTokenService.parseToken(token);
                List<GrantedAuthority> authorities =
                    List.of(new SimpleGrantedAuthority("ROLE_" + principal.role().name()));
                var authentication =
                    new UsernamePasswordAuthenticationToken(principal, null, authorities);
                // Build-and-replace, not mutate-in-place: Spring Security 6+'s
                // SecurityContextHolderFilter installs a *deferred* context
                // supplier (requireExplicitSave semantics) — mutating the
                // object SecurityContextHolder.getContext() happens to return
                // right now is not guaranteed to be visible to later filters.
                // SecurityContextHolder.setContext(...) is the supported way
                // to install an authentication that the rest of the chain
                // (AnonymousAuthenticationFilter, AuthorizationFilter) will see.
                SecurityContext context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(authentication);
                SecurityContextHolder.setContext(context);
            } catch (JwtException | IllegalArgumentException e) {
                // Malformed/expired/tampered token — leave context empty, see class Javadoc.
                log.debug("Rejected invalid bearer token: {}", e.getMessage());
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }
}
