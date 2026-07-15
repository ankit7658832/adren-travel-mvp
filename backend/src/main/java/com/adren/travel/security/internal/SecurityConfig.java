package com.adren.travel.security.internal;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.session.DisableEncodeUrlFilter;
import org.springframework.web.cors.CorsConfigurationSource;
import tools.jackson.databind.ObjectMapper;

/**
 * Stateless JWT security (RULES.md §5.1). No session, no CSRF (a
 * bearer-token API has no cookie-based session for CSRF to attack), every
 * endpoint authenticated by default except health/info actuator endpoints.
 * <p>
 * {@code @EnableMethodSecurity} turns on {@code @PreAuthorize} — the actual
 * role-matrix expressions land per module Api interface in FND-02; this
 * story only needs the annotation processing enabled so those land on
 * already-secured infrastructure, not bolted onto an unauthenticated app.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityConfig {

    private static final String[] PUBLIC_ENDPOINTS = {
        "/actuator/health", "/actuator/health/**", "/actuator/info"
    };

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter,
                                     TraceIdFilter traceIdFilter,
                                     RestAuthenticationEntryPoint authenticationEntryPoint,
                                     RestAccessDeniedHandler accessDeniedHandler,
                                     CorsConfigurationSource corsConfigurationSource) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            // RULES.md §5.4 / FND-08 — resolved per-request from the
            // whitelabel domain registry (DynamicCorsConfigurationSource);
            // never a static wildcard/allow-all here.
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                .anyRequest().authenticated())
            .exceptionHandling(handling -> handling
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler))
            // Traced first (RULES.md §6.1: "generate at the edge... before
            // Spring Security"), so even a 401/403 response carries a traceId.
            .addFilterBefore(traceIdFilter, DisableEncodeUrlFilter.class)
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    JwtAuthenticationFilter jwtAuthenticationFilter(JwtTokenService jwtTokenService) {
        return new JwtAuthenticationFilter(jwtTokenService);
    }

    @Bean
    TraceIdFilter traceIdFilter() {
        return new TraceIdFilter();
    }

    @Bean
    RestAuthenticationEntryPoint restAuthenticationEntryPoint(ObjectMapper objectMapper) {
        return new RestAuthenticationEntryPoint(objectMapper);
    }

    @Bean
    RestAccessDeniedHandler restAccessDeniedHandler(ObjectMapper objectMapper) {
        return new RestAccessDeniedHandler(objectMapper);
    }
}
